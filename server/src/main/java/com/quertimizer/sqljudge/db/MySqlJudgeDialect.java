package com.quertimizer.sqljudge.db;

import java.util.List;

/**
 * Provides MySQL runtime SQL for sql-judge.
 */
public class MySqlJudgeDialect implements SqlJudgeDialect {

    /**
     * Quotes a MySQL identifier.
     *
     * @param identifier database identifier
     * @return quoted database identifier
     */
    @Override
    public String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * Creates SQL for creating a runtime schema.
     *
     * @param environmentName runtime environment name
     * @return runtime schema creation SQL
     */
    @Override
    public String createEnvironmentSql(String environmentName) {
        return "CREATE SCHEMA " + quoteIdentifier(environmentName);
    }

    /**
     * Creates SQL statements for selecting a runtime schema.
     *
     * @param environmentName runtime environment name
     * @return SQL statements for selecting a runtime schema
     */
    @Override
    public List<String> useEnvironmentSqls(String environmentName) {
        return List.of("USE " + quoteIdentifier(environmentName));
    }

    /**
     * Creates SQL for dropping a runtime schema if it exists.
     *
     * @param environmentName runtime environment name
     * @return runtime schema drop SQL
     */
    @Override
    public String dropEnvironmentIfExistsSql(String environmentName) {
        return "DROP SCHEMA IF EXISTS " + quoteIdentifier(environmentName);
    }

    /**
     * Creates SQL statements for applying statement timeout.
     *
     * @param timeoutSeconds timeout in seconds
     * @return SQL statements for applying statement timeout
     */
    @Override
    public List<String> statementTimeoutSqls(int timeoutSeconds) {
        return List.of("SET SESSION MAX_EXECUTION_TIME = " + timeoutSeconds * 1000L);
    }
}
