package com.quertimizer.problem.application.input;

import com.quertimizer.global.constant.DbmsType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProblemOutputPreviewInput {

    private final DbmsType dbmsType;
    private final String ddl;
    private final String sampleDataSql;
    private final String answerSql;
    private final String requester;
    private final String clientIp;
}
