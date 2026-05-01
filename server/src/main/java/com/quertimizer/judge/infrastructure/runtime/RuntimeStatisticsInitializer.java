package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.infrastructure.dialect.JudgeDialect;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

final class RuntimeStatisticsInitializer {

    void initialize(Connection connection, JudgeDialect dialect, String environmentName) throws Exception {
        initialize(null, null, connection, dialect, environmentName);
    }

    void initialize(JudgeExecutionId executionId, ConcurrentHashMap<JudgeExecutionId, Statement> activeStatements,
                    Connection connection, JudgeDialect dialect, String environmentName) throws Exception {
        List<String> statisticsSqls = dialect.initializeStatisticsSqls(environmentName);
        if (statisticsSqls.isEmpty()) {
            String analyzeTablesSql = dialect.analyzeTablesSql(fetchRuntimeTableNames(connection, dialect, environmentName));
            statisticsSqls = analyzeTablesSql.isBlank() ? List.of() : List.of(analyzeTablesSql);
        }

        for (String statisticsSql : statisticsSqls) {
            executeStatisticsStatement(executionId, activeStatements, connection, statisticsSql);
        }
    }

    private List<String> fetchRuntimeTableNames(Connection connection, JudgeDialect dialect,
                                                String environmentName) throws Exception {
        String tableNamesSql = dialect.tableNamesSql(environmentName);
        if (tableNamesSql.isBlank()) {
            return List.of();
        }

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
        Statement statement = connection.createStatement();
        if (executionId != null && activeStatements != null) {
            activeStatements.put(executionId, statement);
        }

        try (statement) {
            statement.execute(statisticsSql);
        } finally {
            if (executionId != null && activeStatements != null) {
                activeStatements.remove(executionId, statement);
            }
        }
    }
}
