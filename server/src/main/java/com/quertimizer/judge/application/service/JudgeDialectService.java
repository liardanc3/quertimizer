package com.quertimizer.judge.application.service;

import com.quertimizer.judge.domain.model.DbmsType;

public class JudgeDialectService {

    private final JudgeDialect postgreSqlDialect = new PostgreSqlDialect();
    private final JudgeDialect mySqlDialect = new MySqlDialect();
    public JudgeDialect get(DbmsType dbmsType) {
        return switch (dbmsType) {
            case POSTGRESQL -> postgreSqlDialect;
            case MYSQL -> mySqlDialect;
        };
    }
}
