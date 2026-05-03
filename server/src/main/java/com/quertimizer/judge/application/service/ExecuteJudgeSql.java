package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.ExecuteJudgeSqlUseCase;
import com.quertimizer.judge.application.input.ExecuteJudgeSqlInput;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.out.JudgeRuntimePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecuteJudgeSql implements ExecuteJudgeSqlUseCase {

    private final JudgeRuntimePort judgeRuntime;

    /**
     * judge 영속 실행 환경에서 SQL을 실행한다.
     *
     * @param input SQL 실행 입력
     * @return SQL 실행 결과
     */
    @Override
    public SqlExecutionResult execute(ExecuteJudgeSqlInput input) {
        return judgeRuntime.execute(input);
    }
}
