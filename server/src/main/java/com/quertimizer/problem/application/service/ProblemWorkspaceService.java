package com.quertimizer.problem.application.service;

import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.application.store.ProblemStore;
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

import static com.quertimizer.problem.domain.model.ProblemWorkspaceFailReason.PROBLEM_NOT_FOUND;
import static com.quertimizer.problem.domain.model.ProblemWorkspaceFailReason.TABLE_REQUIRED;
import static com.quertimizer.problem.domain.model.ProblemWorkspaceFailReason.WORKSPACE_PREPARATION_FAILED;
import static com.quertimizer.problem.domain.model.ProblemLogMessage.RESIDUAL_WORKSPACE_CLEANUP_FAILED;
import static com.quertimizer.problem.domain.model.ProblemLogMessage.WORKSPACE_CLEANUP_FAILED;

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

    public WorkspaceHandle prepareWorkspace(String handle, String problemId, String socketId) {
        // 인터랙티브 실행용 작업 스키마를 준비
        Problem problem = problemStore.findProblem(problemId)
                .orElseThrow(() -> new IllegalArgumentException(PROBLEM_NOT_FOUND.getMessage()));

        String workspaceKey = createWorkspaceKey(handle, problemId);
        WorkspaceContext workspaceContext = workspaceByKey.computeIfAbsent(
                workspaceKey,
                key -> new WorkspaceContext(
                        workspaceKey,
                        createSchemaName(handle, problemId),
                        problem.getBaseProblemSetId(),
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
        // 소켓 활동 시각을 갱신
        findWorkspaceBySocketId(socketId)
                .ifPresent(workspaceContext -> workspaceContext.lastActivityAt = Instant.now());
    }

    public void handleConnectionClose(String socketId) {
        // 연결 종료에 맞춰 작업 스키마를 정리 예약
        releaseWorkspace(socketId, false);
    }

    public void handleExplicitLeave(String socketId) {
        // 명시적 이탈 시 작업 스키마를 즉시 정리
        releaseWorkspace(socketId, true);
    }

    public void cleanupInactiveWorkspaces() {
        // 장시간 비활성 작업 스키마를 정리
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
            log.warn(RESIDUAL_WORKSPACE_CLEANUP_FAILED.getMessage(), exception);
        }
    }

    @PreDestroy
    public void shutdownCleanupExecutor() {
        // 작업 스키마 정리 스레드를 종료
        cleanupExecutor.shutdownNow();
    }

    private void releaseWorkspace(String socketId, boolean cleanupImmediately) {
        // 소켓 연결 종료 후 작업 스키마 상태 정리
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
        // 작업 스키마 정리 예약
        cancelCleanup(workspaceContext);
        workspaceContext.cleanupFuture = cleanupExecutor.schedule(
                () -> cleanupWorkspaceByKey(workspaceContext.workspaceKey),
                DISCONNECTED_CLEANUP_DELAY.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private void cleanupWorkspaceByKey(String workspaceKey) {
        // 작업 스키마 정리 대상 조회
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
            log.warn(WORKSPACE_CLEANUP_FAILED.getMessage(), workspaceContext.schemaName, exception);
            return;
        }

        cancelCleanup(workspaceContext);
        workspaceByKey.remove(workspaceContext.workspaceKey, workspaceContext);
    }

    private Optional<WorkspaceContext> findWorkspaceBySocketId(String socketId) {
        // 소켓 연결 기준 작업 스키마 조회
        String workspaceKey = workspaceKeyBySocketId.get(socketId);
        if (workspaceKey == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(workspaceByKey.get(workspaceKey));
    }

    private void createWorkspaceIfMissing(WorkspaceContext workspaceContext) {
        // 작업용 스키마 생성
        String baseSchemaName = "problem_set_" + workspaceContext.problemSetId;

        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement()) {
            List<String> tableNames = workspaceContext.tableNames;
            if (tableNames.isEmpty()) {
                throw new IllegalArgumentException(TABLE_REQUIRED.getMessage());
            }

            if (schemaExists(connection, workspaceContext.schemaName)) {
                if (hasAllWorkspaceTables(connection, workspaceContext.schemaName, tableNames)) {
                    return;
                }

                dropSchema(connection, workspaceContext.schemaName);
            }

            // 작업용 스키마 생성
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + quoteIdentifier(workspaceContext.schemaName));

            for (String tableName : tableNames) {
                createWorkspaceTable(connection, baseSchemaName, workspaceContext.schemaName, tableName);
            }
        } catch (Exception exception) {
            throw new IllegalStateException(WORKSPACE_PREPARATION_FAILED.getMessage(), exception);
        }
    }

    private boolean schemaExists(Connection connection, String schemaName) throws Exception {
        // 작업 스키마 존재 여부 확인
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


    private boolean hasAllWorkspaceTables(Connection connection, String schemaName, List<String> tableNames) throws Exception {
        // 작업 스키마 테이블 준비 여부 확인
        for (String tableName : tableNames) {
            if (!tableExists(connection, schemaName, tableName)) {
                return false;
            }
        }

        return true;
    }

    private boolean tableExists(Connection connection, String schemaName, String tableName) throws Exception {
        // 작업 스키마 테이블 존재 여부 확인
        try (var preparedStatement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_name = ?
                """)) {
            preparedStatement.setString(1, schemaName);
            preparedStatement.setString(2, tableName);

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
        // 작업 스키마 삭제
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + quoteIdentifier(schemaName) + " CASCADE");
        }
    }

    private void cancelCleanup(WorkspaceContext workspaceContext) {
        // 작업 스키마 정리 예약 취소
        if (workspaceContext.cleanupFuture == null) {
            return;
        }

        workspaceContext.cleanupFuture.cancel(false);
        workspaceContext.cleanupFuture = null;
    }

    private List<String> resolveProblemTableNames(Problem problem) {
        // 문제 DDL 기준 테이블 이름 추출
        String ddl = problem.getDdl();
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

    private String createWorkspaceKey(String handle, String problemId) {
        // 작업 스키마 식별 키 생성
        return sanitizeSchemaPrefix(handle) + ":" + problemId;
    }

    private String createSchemaName(String handle, String problemId) {
        // 작업 스키마 이름 생성
        String normalizedHandle = sanitizeSchemaPrefix(handle);
        return normalizedHandle + "_problem_" + problemId.replace('-', '_');
    }

    private String sanitizeSchemaPrefix(String handle) {
        // 작업 스키마 prefix 정규화
        String sanitizedHandle = handle.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (sanitizedHandle.isBlank()) {
            sanitizedHandle = "user";
        }

        if (Character.isDigit(sanitizedHandle.charAt(0))) {
            sanitizedHandle = "u_" + sanitizedHandle;
        }

        int maxPrefixLength = Math.max(1, 63 - "_problem_00001_00001".length());
        return sanitizedHandle.length() > maxPrefixLength
                ? sanitizedHandle.substring(0, maxPrefixLength)
                : sanitizedHandle;
    }

    private String quoteIdentifier(String identifier) {
        // SQL 식별자 인용
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
            // 작업 스키마 정리 전용 데몬 스레드를 생성
            Thread thread = new Thread(runnable, "problem-workspace-cleanup");
            thread.setDaemon(true);
            return thread;
        }
    }
}
