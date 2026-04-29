package com.quertimizer.sqljudge.db;

import java.util.List;

/**
 * Provides DBMS-specific SQL used by sql-judge runtime execution.
 */
public interface SqlJudgeDialect {

    /**
     * Quotes a database identifier.
     *
     * @param identifier database identifier
     * @return quoted database identifier
     */
    String quoteIdentifier(String identifier);

    /**
     * Creates SQL for creating a runtime environment.
     *
     * @param environmentName runtime environment name
     * @return runtime environment creation SQL
     */
    String createEnvironmentSql(String environmentName);

    /**
     * Creates SQL statements for selecting a runtime environment.
     *
     * @param environmentName runtime environment name
     * @return SQL statements for selecting a runtime environment
     */
    List<String> useEnvironmentSqls(String environmentName);

    /**
     * Creates SQL for dropping a runtime environment if it exists.
     *
     * @param environmentName runtime environment name
     * @return runtime environment drop SQL
     */
    String dropEnvironmentIfExistsSql(String environmentName);

    /**
     * Creates SQL statements for applying execution timeout.
     *
     * @param timeoutSeconds timeout in seconds
     * @return SQL statements for applying execution timeout
     */
    List<String> statementTimeoutSqls(int timeoutSeconds);
}
