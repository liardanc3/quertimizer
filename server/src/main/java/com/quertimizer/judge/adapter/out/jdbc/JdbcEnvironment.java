package com.quertimizer.judge.adapter.out.jdbc;

import com.quertimizer.judge.adapter.out.jdbc.dialect.SqlDialectProvider;
import com.quertimizer.judge.application.model.DatabaseLease;
import com.quertimizer.judge.application.model.EnvironmentConnection;
import com.quertimizer.judge.application.model.ExecutionEnvironment;
import com.quertimizer.judge.application.port.out.EnvironmentConnectionPort;
import com.quertimizer.judge.application.port.out.SqlDialect;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_DB_PROCESS_READY_FAILED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_ENVIRONMENT_CONNECTION_FAILED;

@Component
@RequiredArgsConstructor
public class JdbcEnvironment implements EnvironmentConnectionPort {

    private final SqlDialectProvider dialectProvider;

    @Override
    public void waitUntilReady(ExecutionEnvironment environment, int startupTimeoutSeconds) {
        // 런타임 DB 프로세스가 JDBC 연결을 받을 때까지 대기
        DatabaseLease lease = null;
        Connection connection = null;
        Exception lastFailure = null;
        Instant deadline = Instant.now().plusSeconds(startupTimeoutSeconds);
        while (!Instant.now().isAfter(deadline)) {
            try {
                lease = new DatabaseLease(environment.getDatabase());
                connection = lease.openConnection();
                connection.setAutoCommit(false);

                configureExecutionConnection(
                        connection, dialectProvider.get(environment.getDatabase().getDbmsType()),
                        environment.getName().getValue(), startupTimeoutSeconds
                );
                rollback(connection);
                closeQuietly(connection);
                closeQuietly(lease);
                return;
            } catch (Exception exception) {
                lastFailure = exception;
                closeQuietly(connection);
                closeQuietly(lease);
                sleepBeforeRetry("LVM 스냅샷 DB 프로세스 대기 중 중단");
            } finally {
                connection = null;
                lease = null;
            }
        }

        throw new IllegalStateException(LVM_DB_PROCESS_READY_FAILED.getMessage(), lastFailure);
    }

    @Override
    public EnvironmentConnection open(ExecutionEnvironment environment, int timeoutSeconds) {
        // 실행 환경 JDBC 연결 생성과 schema, timeout 설정
        DatabaseLease lease = null;
        Connection connection = null;
        try {
            lease = new DatabaseLease(environment.getDatabase());
            connection = lease.openConnection();
            connection.setAutoCommit(false);

            SqlDialect dialect = dialectProvider.get(environment.getDatabase().getDbmsType());
            configureExecutionConnection(connection, dialect, environment.getName().getValue(), timeoutSeconds);
            return new EnvironmentConnection(lease, connection, dialect, environment.getName().getValue());
        } catch (Exception exception) {
            closeQuietly(connection);
            closeQuietly(lease);
            throw new IllegalStateException(LVM_ENVIRONMENT_CONNECTION_FAILED.getMessage(), exception);
        }
    }

    public void initializeStatistics(Connection connection, SqlDialect dialect, String environmentName) throws Exception {
        initializeStatistics(null, null, connection, dialect, environmentName);
    }

    public void initializeStatistics(JudgeExecutionId executionId,
                                     ConcurrentHashMap<JudgeExecutionId, Statement> activeStatements,
                                     Connection connection, SqlDialect dialect,
                                     String environmentName) throws Exception {
        // 실행 환경 테이블 목록 기준 통계 옵션과 통계 초기화 SQL 구성
        List<String> tableNames = fetchEnvironmentTableNames(connection, dialect, environmentName);
        List<String> statisticsSqls = new ArrayList<>(dialect.persistentStatisticsSqls(tableNames));
        List<String> initializeStatisticsSqls = dialect.initializeStatisticsSqls(environmentName);
        if (initializeStatisticsSqls.isEmpty()) {
            String analyzeTablesSql = dialect.analyzeTablesSql(tableNames);
            if (!analyzeTablesSql.isBlank()) {
                statisticsSqls.add(analyzeTablesSql);
            }
        } else {
            statisticsSqls.addAll(initializeStatisticsSqls);
        }

        // 통계 SQL 순차 실행
        for (String statisticsSql : statisticsSqls) {
            executeStatisticsStatement(executionId, activeStatements, connection, statisticsSql);
        }
    }

    private void configureExecutionConnection(Connection connection, SqlDialect dialect,
                                              String environmentName, int timeoutSeconds) throws Exception {
        // 실행 schema 선택과 timeout 설정
        try (var statement = connection.createStatement()) {
            for (String useEnvironmentSql : dialect.useEnvironmentSqls(environmentName)) {
                statement.execute(useEnvironmentSql);
            }
            for (String timeoutSql : dialect.statementTimeoutSqls(timeoutSeconds)) {
                statement.execute(timeoutSql);
            }
        }
    }

    private List<String> fetchEnvironmentTableNames(Connection connection, SqlDialect dialect,
                                                    String environmentName) throws Exception {
        // 테이블 목록 조회 SQL 미지원 시 빈 목록 반환
        String tableNamesSql = dialect.tableNamesSql(environmentName);
        if (tableNamesSql.isBlank()) {
            return List.of();
        }

        // 실행 환경의 기본 테이블명 조회
        List<String> tableNames = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(tableNamesSql)) {
            while (resultSet.next()) {
                tableNames.add(resultSet.getString(1));
            }
        }

        return tableNames;
    }

    private void executeStatisticsStatement(JudgeExecutionId executionId,
                                            ConcurrentHashMap<JudgeExecutionId, Statement> activeStatements,
                                            Connection connection, String statisticsSql) throws Exception {
        // 통계 문장 생성과 실행 중 문장 추적
        Statement statement = connection.createStatement();
        if (executionId != null && activeStatements != null) {
            activeStatements.put(executionId, statement);
        }

        // 통계 문장 실행 후 추적 제거
        try (statement) {
            statement.execute(statisticsSql);
        } finally {
            if (executionId != null && activeStatements != null) {
                activeStatements.remove(executionId, statement);
            }
        }
    }

    private void sleepBeforeRetry(String interruptedMessage) {
        // 런타임 DB 준비 상태 재시도 간격 대기
        try {
            Thread.sleep(500L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interruptedMessage, exception);
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    private void closeQuietly(DatabaseLease lease) {
        if (lease != null) {
            lease.close();
        }
    }
}
