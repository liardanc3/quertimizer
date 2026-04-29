package com.quertimizer.judge.application.input;

import com.quertimizer.global.constant.DbmsType;

public record ProblemOutputPreviewInput(DbmsType dbmsType,
                                        String ddl,
                                        String sampleDataSql,
                                        String answerSql,
                                        String requester,
                                        String clientIp) {
}
