package com.quertimizer.judge.infrastructure.template;

import com.quertimizer.global.constant.DbmsType;

public record TemplateDatabaseConnectionInfo(DbmsType dbmsType,
                                             String name,
                                             String url,
                                             String username,
                                             String password) {
}
