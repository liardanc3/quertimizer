package com.quertimizer.judge.application.input;

import com.quertimizer.global.constant.DbmsType;

public record GenerateAnswerHashInput(DbmsType dbmsType,
                                      String ddl,
                                      String actualDataSql,
                                      String answerSql) {
}
