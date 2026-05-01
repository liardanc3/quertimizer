package com.quertimizer.judge.infrastructure.dialect;

import com.quertimizer.judge.domain.model.DbmsType;

public class JudgeDialectProvider {

    private final JudgeDialect postgreSqlDialect = new PostgreSqlDialect();
    private final JudgeDialect mySqlDialect = new MySqlDialect();

    public JudgeDialect get(DbmsType dbmsType) {
        return switch (dbmsType) {
            case POSTGRESQL -> postgreSqlDialect;
            case MYSQL -> mySqlDialect;
        };
    }
}
