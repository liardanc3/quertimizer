package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;

public record ExecutionDatabaseConnectionInfo(DbmsType dbmsType,
                                              String name,
                                              String url,
                                              String username,
                                              String password) {
}
