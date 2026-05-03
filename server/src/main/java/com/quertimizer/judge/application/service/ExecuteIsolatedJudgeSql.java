package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.ExecuteIsolatedJudgeSqlUseCase;
import com.quertimizer.judge.application.input.ExecuteIsolatedJudgeSqlInput;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.out.JudgeRuntimePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecuteIsolatedJudgeSql implements ExecuteIsolatedJudgeSqlUseCase {

    private final JudgeRuntimePort judgeRuntime;

    /**
     * judge 격리 실행 환경에서 SQL을 실행한다.
     *
     * @param input 격리 SQL 실행 입력
     * @return SQL 실행 결과
     */
    @Override
    public SqlExecutionResult execute(ExecuteIsolatedJudgeSqlInput input) {
        return judgeRuntime.executeIsolated(input);
    }
}
