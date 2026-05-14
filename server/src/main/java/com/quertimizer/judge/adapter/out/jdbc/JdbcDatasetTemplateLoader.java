package com.quertimizer.judge.adapter.out.jdbc;

import com.quertimizer.judge.application.port.out.SqlDialect;
import com.quertimizer.judge.adapter.out.jdbc.dialect.SqlDialectProvider;
import com.quertimizer.judge.application.port.out.DatasetLoaderPort;
import com.quertimizer.judge.application.model.Database;
import com.quertimizer.judge.application.model.DatabaseLease;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.service.SqlStatementParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static com.quertimizer.judge.domain.model.JudgeFailReason.DATASET_TEMPLATE_TABLE_NOT_CREATED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_TEMPLATE_DB_PROCESS_READY_FAILED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_TEMPLATE_DB_PROCESS_WAIT_INTERRUPTED;

@Component
@RequiredArgsConstructor
public class JdbcDatasetTemplateLoader implements DatasetLoaderPort {

    private final SqlDialectProvider dialectProvider;
    private final SqlStatementParser statementParser;
    private final JdbcEnvironment environment;

    @Override
    public void waitUntilReady(Database templateDatabase, int startupTimeoutSeconds) {
        // 템플릿 DB 프로세스가 JDBC 연결을 받을 때까지 대기
        Exception lastFailure = null;
        Instant deadline = Instant.now().plusSeconds(startupTimeoutSeconds);
        while (!Instant.now().isAfter(deadline)) {
            try (DatabaseLease lease = new DatabaseLease(templateDatabase);
                 Connection connection = lease.openConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
                return;
            } catch (Exception exception) {
                lastFailure = exception;
                sleepBeforeRetry();
            }
        }

        throw new IllegalStateException(LVM_TEMPLATE_DB_PROCESS_READY_FAILED.getMessage(), lastFailure);
    }

    @Override
    public void load(Database templateDatabase, String environmentName, DatasetDefinition dataset) throws Exception {
        // 템플릿 DB에 실행 환경 생성 후 DDL, 데이터, 기본 인덱스, 통계 순서로 적재
        SqlDialect dialect = dialectProvider.get(templateDatabase.getDbmsType());
        try (DatabaseLease lease = new DatabaseLease(templateDatabase);
             Connection connection = lease.openConnection()) {
            connection.setAutoCommit(false);
            try {
                createEnvironment(connection, dialect, environmentName);
                executeStatements(connection, dataset.getDdl());
                executeStatements(connection, dataset.getDataSql());
                for (String baseIndexDdl : dataset.getBaseIndexDdls()) {
                    executeStatements(connection, baseIndexDdl);
                }
                verifyEnvironmentTablesCreated(connection, dialect, environmentName);
                environment.initializeStatistics(connection, dialect, environmentName);
                connection.commit();
            } catch (Exception exception) {
                rollback(connection);
                throw exception;
            }
        }
    }

    private void verifyEnvironmentTablesCreated(Connection connection, SqlDialect dialect,
                                                String environmentName) throws Exception {
        // 템플릿 schema에 실제 테이블 생성 여부 확인
        String tableNamesSql = dialect.tableNamesSql(environmentName);
        if (tableNamesSql.isBlank()) {
            return;
        }

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(tableNamesSql)) {
            if (!resultSet.next()) {
                throw new IllegalStateException(DATASET_TEMPLATE_TABLE_NOT_CREATED.getMessage());
            }
        }
    }

    private void createEnvironment(Connection connection, SqlDialect dialect, String environmentName) throws Exception {
        // 템플릿 적재용 schema 생성과 선택
        try (Statement statement = connection.createStatement()) {
            statement.execute(dialect.dropEnvironmentIfExistsSql(environmentName));
            statement.execute(dialect.createEnvironmentSql(environmentName));
            for (String useEnvironmentSql : dialect.useEnvironmentSqls(environmentName)) {
                statement.execute(useEnvironmentSql);
            }
        }
    }

    private void executeStatements(Connection connection, String sql) throws Exception {
        // SQL 문자열을 문장 단위로 분리해 순차 실행
        for (String statementSql : statementParser.splitStatements(sql)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private void sleepBeforeRetry() {
        // 템플릿 DB 준비 상태 재시도 간격 대기
        try {
            Thread.sleep(500L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(LVM_TEMPLATE_DB_PROCESS_WAIT_INTERRUPTED.getMessage(), exception);
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (Exception ignored) {
        }
    }
}
