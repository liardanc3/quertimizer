package com.quertimizer.judge.application.input;

import com.quertimizer.global.constant.DbmsType;

public record RefreshTemplateDatasetInput(String problemSetId,
                                          DbmsType dbmsType,
                                          String ddl,
                                          String actualDataSql,
                                          String templateVersion) {
}
