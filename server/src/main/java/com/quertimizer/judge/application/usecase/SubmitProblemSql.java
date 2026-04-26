package com.quertimizer.judge.application.usecase;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.application.service.JudgeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class SubmitProblemSql {

    private final JudgeQueryService judgeQueryService;

    public JudgeQueryService.ProblemSubmitResult execute(String handle,
                                                         String socketId,
                                                         String problemId,
                                                         String sql,
                                                         DbmsType dbmsType,
                                                         Consumer<JudgeQueryService.ProblemSubmitProgress> progressListener) {
        // 문제 제출과 채점 진입점을 judge 도메인 use case로 처리
        return judgeQueryService.submitProblemSql(handle, socketId, problemId, sql, dbmsType, progressListener);
    }
}
