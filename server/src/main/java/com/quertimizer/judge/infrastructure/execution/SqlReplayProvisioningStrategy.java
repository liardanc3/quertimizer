package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.judge.domain.service.JudgeSqlStatementParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
public class SqlReplayProvisioningStrategy implements DatasetProvisioningStrategy {

    private final JudgeSqlStatementParser judgeSqlStatementParser;

    @Override
    public void provision(Connection connection, String schemaName, String ddl, String dataSql) throws Exception {
        // DDL + data SQL을 execution schema에서 매번 재생해 dataset을 준비
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + quoteIdentifier(schemaName));
            statement.execute("SET LOCAL search_path TO " + quoteIdentifier(schemaName) + ", public");
        }

        executeStatements(connection, ddl);
        executeStatements(connection, dataSql);

        // TODO 통계 상태 안정화 정책이 정해지면 ANALYZE/통계 복제 전략을 분리한다.
    }

    private void executeStatements(Connection connection, String sql) throws Exception {
        for (String statementSql : judgeSqlStatementParser.splitStatements(sql)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
