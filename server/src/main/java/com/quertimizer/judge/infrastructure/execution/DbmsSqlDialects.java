package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.application.port.DbmsSqlDialect;
import com.quertimizer.judge.application.port.DbmsSqlDialectProvider;
import org.springframework.stereotype.Component;

@Component
public class DbmsSqlDialects implements DbmsSqlDialectProvider {

    private final DbmsSqlDialect postgreSqlDialect = new PostgreSqlDialect();
    private final DbmsSqlDialect mySqlDialect = new MySqlDialect();

    @Override
    public DbmsSqlDialect get(DbmsType dbmsType) {
        // DBMS별 SQL dialect 조회
        return switch (dbmsType) {
            case POSTGRESQL -> postgreSqlDialect;
            case MYSQL -> mySqlDialect;
        };
    }
}
