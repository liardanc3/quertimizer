package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import org.springframework.stereotype.Component;

@Component
public class DbmsSqlDialects {

    private final DbmsSqlDialect postgreSqlDialect = new PostgreSqlDialect();
    private final DbmsSqlDialect mySqlDialect = new MySqlDialect();

    public DbmsSqlDialect get(DbmsType dbmsType) {
        // DBMS별 SQL dialect 조회
        return switch (dbmsType) {
            case POSTGRESQL -> postgreSqlDialect;
            case MYSQL -> mySqlDialect;
        };
    }
}
