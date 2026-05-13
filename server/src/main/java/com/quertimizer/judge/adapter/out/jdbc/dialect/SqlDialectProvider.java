package com.quertimizer.judge.adapter.out.jdbc.dialect;

import com.quertimizer.judge.application.port.out.SqlDialect;
import com.quertimizer.judge.domain.model.DbmsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SqlDialectProvider {

    private final PostgreSqlDialect postgreSqlDialect;
    private final MySqlDialect mySqlDialect;

    public SqlDialect get(DbmsType dbmsType) {
        return switch (dbmsType) {
            case POSTGRESQL -> postgreSqlDialect;
            case MYSQL -> mySqlDialect;
        };
    }
}
