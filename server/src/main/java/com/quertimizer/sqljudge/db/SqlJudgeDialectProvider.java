package com.quertimizer.sqljudge.db;

/**
 * Provides DBMS-specific sql-judge dialects.
 */
public class SqlJudgeDialectProvider {

    private final SqlJudgeDialect postgreSqlDialect = new PostgreSqlJudgeDialect();
    private final SqlJudgeDialect mySqlDialect = new MySqlJudgeDialect();

    /**
     * Returns a DBMS-specific sql-judge dialect.
     *
     * @param dbmsType DBMS type
     * @return DBMS-specific sql-judge dialect
     */
    public SqlJudgeDialect get(DbmsType dbmsType) {
        return switch (dbmsType) {
            case POSTGRESQL -> postgreSqlDialect;
            case MYSQL -> mySqlDialect;
        };
    }
}
