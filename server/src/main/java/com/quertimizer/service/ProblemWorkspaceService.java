package com.quertimizer.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemWorkspaceService {

    private static final Duration DISCONNECTED_CLEANUP_DELAY = Duration.ofMinutes(10);
    private static final Duration WORKSPACE_INACTIVITY_TIMEOUT = Duration.ofMinutes(30);

    private final DataSource dataSource;
    private final ProblemStore problemStore;

    private final Map<String, WorkspaceContext> workspaceByKey = new ConcurrentHashMap<>();
    private final Map<String, String> workspaceKeyBySocketId = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(new CleanupThreadFactory());

    public WorkspaceHandle prepareWorkspace(String userId, String problemId, String socketId) {

        // 문제 존재 여부 확인
        problemStore.findProblem(problemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제다."));

        String datasetId = resolveDatasetId(problemId);
        String workspaceKey = createSchemaName(userId, datasetId);
        WorkspaceContext workspaceContext = workspaceByKey.computeIfAbsent(
                workspaceKey,
                key -> new WorkspaceContext(workspaceKey, createSchemaName(userId, datasetId), datasetId)
        );

        synchronized (workspaceContext.monitor) {

            // 연결된 소켓과 마지막 활동 시각 갱신
            cancelCleanup(workspaceContext);
            workspaceContext.activeSocketIds.add(socketId);
            workspaceContext.lastActivityAt = Instant.now();
            workspaceKeyBySocketId.put(socketId, workspaceKey);

            // 작업용 스키마가 없으면 복제본 생성
            createWorkspaceIfMissing(workspaceContext);
        }

        return new WorkspaceHandle(workspaceContext.schemaName, datasetId);
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

        // 서버 재시작 후 남은 작업용 스키마 정리
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT schema_name
                     FROM information_schema.schemata
                     WHERE schema_name LIKE '%\\_problem_set\\_%' ESCAPE '\\'
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

            // 연결 종료된 소켓 제거
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
        String baseSchemaName = "problem_set_" + workspaceContext.datasetId;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            if (schemaExists(connection, workspaceContext.schemaName)) {
                return;
            }

            // 작업용 스키마 생성
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + quoteIdentifier(workspaceContext.schemaName));

            List<String> tableNames = findBaseTableNames(connection, baseSchemaName);
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

    private List<String> findBaseTableNames(Connection connection, String baseSchemaName) throws Exception {
        try (var preparedStatement = connection.prepareStatement("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """)) {
            preparedStatement.setString(1, baseSchemaName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<String> tableNames = new ArrayList<>();
                while (resultSet.next()) {
                    tableNames.add(resultSet.getString("table_name"));
                }

                if (tableNames.isEmpty()) {
                    throw new IllegalArgumentException("문제 템플릿 테이블셋을 찾을 수 없다.");
                }

                return tableNames;
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

    private String resolveDatasetId(String problemId) {
        String[] tokens = problemId.split("-");
        if (tokens.length < 2 || tokens[0].isBlank()) {
            throw new IllegalArgumentException("잘못된 문제 번호다.");
        }

        return tokens[0];
    }

    private String createSchemaName(String userId, String datasetId) {
        String normalizedUserId = sanitizeSchemaPrefix(userId);
        return normalizedUserId + "_problem_set_" + datasetId;
    }

    private String sanitizeSchemaPrefix(String userId) {
        String sanitizedUserId = userId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (sanitizedUserId.isBlank()) {
            sanitizedUserId = "user";
        }

        if (Character.isDigit(sanitizedUserId.charAt(0))) {
            sanitizedUserId = "u_" + sanitizedUserId;
        }

        int maxPrefixLength = Math.max(1, 63 - "_problem_set_00000".length());
        return sanitizedUserId.length() > maxPrefixLength
                ? sanitizedUserId.substring(0, maxPrefixLength)
                : sanitizedUserId;
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public record WorkspaceHandle(String schemaName, String datasetId) {
    }

    private static final class WorkspaceContext {
        private final String workspaceKey;
        private final String schemaName;
        private final String datasetId;
        private final Set<String> activeSocketIds = ConcurrentHashMap.newKeySet();
        private final Object monitor = new Object();
        private Instant lastActivityAt = Instant.now();
        private ScheduledFuture<?> cleanupFuture;

        private WorkspaceContext(String workspaceKey, String schemaName, String datasetId) {
            this.workspaceKey = workspaceKey;
            this.schemaName = schemaName;
            this.datasetId = datasetId;
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
