package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.AnalyzeJudgeEnvironmentUseCase;
import com.quertimizer.judge.application.input.AnalyzeJudgeEnvironmentInput;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.out.JudgeRuntimePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyzeJudgeEnvironment implements AnalyzeJudgeEnvironmentUseCase {

    private final JudgeRuntimePort judgeRuntime;

    /**
     * judge 실행 환경의 DBMS 통계를 갱신한다.
     *
     * @param input 통계 갱신 입력
     * @return 통계 갱신 실행 결과
     */
    @Override
    public SqlExecutionResult execute(AnalyzeJudgeEnvironmentInput input) {
        return judgeRuntime.analyze(input);
    }
}
