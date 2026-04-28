package com.quertimizer.judge.application.input;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.application.service.JudgeQueryService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

@Getter
@RequiredArgsConstructor
public class SubmitProblemSqlInput {

    private final String handle;
    private final String socketId;
    private final String problemId;
    private final String sql;
    private final DbmsType dbmsType;
    private final Consumer<JudgeQueryService.ProblemSubmitProgress> progressListener;
}
