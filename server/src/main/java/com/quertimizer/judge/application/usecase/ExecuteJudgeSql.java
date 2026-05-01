package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.input.ExecuteJudgeSqlInput;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.JudgeRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * judge 영속 실행 환경에서 SQL을 실행한다.
 */
@Component
@RequiredArgsConstructor
public class ExecuteJudgeSql {

    private final JudgeRuntime judgeRuntime;

    /**
     * judge 영속 실행 환경에서 SQL을 실행한다.
     *
     * @param input SQL 실행 입력
     * @return SQL 실행 결과
     */
    public SqlExecutionResult execute(ExecuteJudgeSqlInput input) {
        return judgeRuntime.execute(input);
    }
}
