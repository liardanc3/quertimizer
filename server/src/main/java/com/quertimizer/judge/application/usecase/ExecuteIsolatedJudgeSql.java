package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.input.ExecuteIsolatedJudgeSqlInput;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.JudgeRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * judge 격리 실행 환경에서 SQL을 실행한다.
 */
@Component
@RequiredArgsConstructor
public class ExecuteIsolatedJudgeSql {

    private final JudgeRuntime judgeRuntime;

    /**
     * judge 격리 실행 환경에서 SQL을 실행한다.
     *
     * @param input 격리 SQL 실행 입력
     * @return SQL 실행 결과
     */
    public SqlExecutionResult execute(ExecuteIsolatedJudgeSqlInput input) {
        return judgeRuntime.executeIsolated(input);
    }
}
