package com.quertimizer.service;

import com.quertimizer.entity.Problem;
import com.quertimizer.store.ProblemStore;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemWorkspaceService {

    private static final Duration DISCONNECTED_CLEANUP_DELAY = Duration.ofMinutes(10);
    private static final Duration WORKSPACE_INACTIVITY_TIMEOUT = Duration.ofMinutes(30);
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("CREATE TABLE\\s+(?:[\\w]+\\.)?(\\w+)\\s*\\(", Pattern.CASE_INSENSITIVE);

    private final DataSource dataSource;
    private final ProblemStore problemStore;

    private final Map<String, WorkspaceContext> workspaceByKey = new ConcurrentHashMap<>();
    private final Map<String, String> workspaceKeyBySocketId = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(new CleanupThreadFactory());

    public WorkspaceHandle prepareWorkspace(String userId, String problemId, String socketId) {
        Problem problem = problemStore.findProblem(problemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제다."));

        String workspaceKey = createWorkspaceKey(userId, problemId);
        WorkspaceContext workspaceContext = workspaceByKey.computeIfAbsent(
                workspaceKey,
                key -> new WorkspaceContext(
                        workspaceKey,
                        createSchemaName(userId, problemId),
                        problem.getResolvedProblemSetId(),
                        resolveProblemTableNames(problem)
                )
        );

        synchronized (workspaceContext.monitor) {

            // 연결 정보, 마지막 활동 시각 갱신
            cancelCleanup(workspaceContext);
            workspaceContext.activeSocketIds.add(socketId);
            workspaceContext.lastActivityAt = Instant.now();
            workspaceKeyBySocketId.put(socketId, workspaceKey);

            // 작업용 스키마가 없으면 문제 기준 subset 복제
            createWorkspaceIfMissing(workspaceContext);
        }

        return new WorkspaceHandle(workspaceContext.schemaName, workspaceContext.problemSetId);
    }

    public void markActivity(String socketId) {
        findWorkspaceBySocketId(socketId)
                .ifPresent(workspaceContext -> workspaceContext.lastActivityAt = Instant.now());
    }

    public void handleConnectionClose(String socketId) {
        releaseWorkspace(socketId, false);
    }

    public void handleExplicitLeave(String socketId) {
        releaseWorkspace(socketId, true);
    }

    public void cleanupInactiveWorkspaces() {
        Instant now = Instant.now();

        // 비활성 작업용 스키마 정리
        for (WorkspaceContext workspaceContext : List.copyOf(workspaceByKey.values())) {
            synchronized (workspaceContext.monitor) {
                if (!workspaceContext.activeSocketIds.isEmpty()) {
                    continue;
                }

                if (Duration.between(workspaceContext.lastActivityAt, now).compareTo(WORKSPACE_INACTIVITY_TIMEOUT) < 0) {
                    continue;
                }

                cleanupWorkspace(workspaceContext);
            }
        }
    }

    public void cleanupResidualWorkspaces() {

        // 서버 재시작 뒤 잔여 작업용 스키마 정리
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT schema_name
                     FROM information_schema.schemata
                     WHERE schema_name LIKE '%\\_problem\\_%\\_%' ESCAPE '\\'
                       AND schema_name NOT LIKE 'problem_set\\_%' ESCAPE '\\'
                     """)) {

            List<String> schemaNames = new ArrayList<>();
            while (resultSet.next()) {
                schemaNames.add(resultSet.getString("schema_name"));
            }

            for (String schemaName : schemaNames) {
                dropSchema(connection, schemaName);
            }
        } catch (Exception exception) {
            log.warn("잔여 작업용 스키마 정리에 실패했다.", exception);
        }
    }

    @PreDestroy
    public void shutdownCleanupExecutor() {
        cleanupExecutor.shutdownNow();
    }

    private void releaseWorkspace(String socketId, boolean cleanupImmediately) {
        String workspaceKey = workspaceKeyBySocketId.remove(socketId);
        if (workspaceKey == null) {
            return;
        }

        WorkspaceContext workspaceContext = workspaceByKey.get(workspaceKey);
        if (workspaceContext == null) {
            return;
        }

        synchronized (workspaceContext.monitor) {

            // 연결 종료 뒤 소켓 정보 제거
            workspaceContext.activeSocketIds.remove(socketId);
            workspaceContext.lastActivityAt = Instant.now();

            if (!workspaceContext.activeSocketIds.isEmpty()) {
                return;
            }

            if (cleanupImmediately) {
                cleanupWorkspace(workspaceContext);
                return;
            }

            scheduleCleanup(workspaceContext);
        }
    }

    private void scheduleCleanup(WorkspaceContext workspaceContext) {
        cancelCleanup(workspaceContext);
        workspaceContext.cleanupFuture = cleanupExecutor.schedule(
                () -> cleanupWorkspaceByKey(workspaceContext.workspaceKey),
                DISCONNECTED_CLEANUP_DELAY.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private void cleanupWorkspaceByKey(String workspaceKey) {
        WorkspaceContext workspaceContext = workspaceByKey.get(workspaceKey);
        if (workspaceContext == null) {
            return;
        }

        synchronized (workspaceContext.monitor) {
            if (!workspaceContext.activeSocketIds.isEmpty()) {
                return;
            }

            cleanupWorkspace(workspaceContext);
        }
    }

    private void cleanupWorkspace(WorkspaceContext workspaceContext) {

        // 작업용 스키마 drop 후 메모리 상태 제거
        try (Connection connection = dataSource.getConnection()) {
            dropSchema(connection, workspaceContext.schemaName);
        } catch (Exception exception) {
            log.warn("작업용 스키마 정리에 실패했다. schema={}", workspaceContext.schemaName, exception);
            return;
        }

        cancelCleanup(workspaceContext);
        workspaceByKey.remove(workspaceContext.workspaceKey, workspaceContext);
    }

    private Optional<WorkspaceContext> findWorkspaceBySocketId(String socketId) {
        String workspaceKey = workspaceKeyBySocketId.get(socketId);
        if (workspaceKey == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(workspaceByKey.get(workspaceKey));
    }

    private void createWorkspaceIfMissing(WorkspaceContext workspaceContext) {
        String baseSchemaName = "problem_set_" + workspaceContext.problemSetId;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            if (schemaExists(connection, workspaceContext.schemaName)) {
                return;
            }

            // 작업용 스키마 생성
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + quoteIdentifier(workspaceContext.schemaName));

            List<String> tableNames = workspaceContext.tableNames;
            if (tableNames.isEmpty()) {
                throw new IllegalArgumentException("문제에서 사용할 테이블이 없다.");
            }

            for (String tableName : tableNames) {
                createWorkspaceTable(connection, baseSchemaName, workspaceContext.schemaName, tableName);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("작업용 스키마를 준비하지 못했다.", exception);
        }
    }

    private boolean schemaExists(Connection connection, String schemaName) throws Exception {
        try (var preparedStatement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.schemata
                WHERE schema_name = ?
                """)) {
            preparedStatement.setString(1, schemaName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private void createWorkspaceTable(Connection connection,
                                      String baseSchemaName,
                                      String workspaceSchemaName,
                                      String tableName) throws Exception {
        try (Statement statement = connection.createStatement()) {

            // 원본 테이블 구조 복제
            statement.execute("""
                    CREATE TABLE %s.%s
                    (LIKE %s.%s INCLUDING DEFAULTS INCLUDING GENERATED INCLUDING IDENTITY INCLUDING CONSTRAINTS INCLUDING COMMENTS)
                    """.formatted(
                    quoteIdentifier(workspaceSchemaName),
                    quoteIdentifier(tableName),
                    quoteIdentifier(baseSchemaName),
                    quoteIdentifier(tableName)
            ));

            // 원본 데이터 복제
            statement.execute("""
                    INSERT INTO %s.%s
                    SELECT *
                    FROM %s.%s
                    """.formatted(
                    quoteIdentifier(workspaceSchemaName),
                    quoteIdentifier(tableName),
                    quoteIdentifier(baseSchemaName),
                    quoteIdentifier(tableName)
            ));

            // 통계 갱신
            statement.execute("ANALYZE " + quoteIdentifier(workspaceSchemaName) + "." + quoteIdentifier(tableName));
        }
    }

    private void dropSchema(Connection connection, String schemaName) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + quoteIdentifier(schemaName) + " CASCADE");
        }
    }

    private void cancelCleanup(WorkspaceContext workspaceContext) {
        if (workspaceContext.cleanupFuture == null) {
            return;
        }

        workspaceContext.cleanupFuture.cancel(false);
        workspaceContext.cleanupFuture = null;
    }

    private List<String> resolveProblemTableNames(Problem problem) {
        String ddl = problem.getDdlPostgresql() != null && !problem.getDdlPostgresql().isBlank()
                ? problem.getDdlPostgresql()
                : problem.getDdlOracle();
        if (ddl == null || ddl.isBlank()) {
            return List.of();
        }

        List<String> tableNames = new ArrayList<>();
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(ddl);
        while (matcher.find()) {
            tableNames.add(matcher.group(1));
        }

        return tableNames.stream().distinct().toList();
    }

    private String createWorkspaceKey(String userId, String problemId) {
        return sanitizeSchemaPrefix(userId) + ":" + problemId;
    }

    private String createSchemaName(String userId, String problemId) {
        String normalizedUserId = sanitizeSchemaPrefix(userId);
        return normalizedUserId + "_problem_" + problemId.replace('-', '_');
    }

    private String sanitizeSchemaPrefix(String userId) {
        String sanitizedUserId = userId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (sanitizedUserId.isBlank()) {
            sanitizedUserId = "user";
        }

        if (Character.isDigit(sanitizedUserId.charAt(0))) {
            sanitizedUserId = "u_" + sanitizedUserId;
        }

        int maxPrefixLength = Math.max(1, 63 - "_problem_00001_00001".length());
        return sanitizedUserId.length() > maxPrefixLength
                ? sanitizedUserId.substring(0, maxPrefixLength)
                : sanitizedUserId;
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public record WorkspaceHandle(String schemaName, String problemSetId) {
    }

    private static final class WorkspaceContext {
        private final String workspaceKey;
        private final String schemaName;
        private final String problemSetId;
        private final List<String> tableNames;
        private final Set<String> activeSocketIds = ConcurrentHashMap.newKeySet();
        private final Object monitor = new Object();
        private Instant lastActivityAt = Instant.now();
        private ScheduledFuture<?> cleanupFuture;

        private WorkspaceContext(String workspaceKey, String schemaName, String problemSetId, List<String> tableNames) {
            this.workspaceKey = workspaceKey;
            this.schemaName = schemaName;
            this.problemSetId = problemSetId;
            this.tableNames = tableNames;
        }
    }

    private static final class CleanupThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "problem-workspace-cleanup");
            thread.setDaemon(true);
            return thread;
        }
    }
}
