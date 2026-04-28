package com.quertimizer.judge.application.port;

import com.quertimizer.global.constant.DbmsType;

public interface DbmsSqlDialectProvider {

    DbmsSqlDialect get(DbmsType dbmsType);
}
