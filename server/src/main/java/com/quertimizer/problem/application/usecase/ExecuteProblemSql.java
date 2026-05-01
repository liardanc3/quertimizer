package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.output.ProblemExecutionOutput;
import com.quertimizer.problem.application.port.ProblemExecutionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 문제 SQL 실행 요청을 실행 포트에 위임한다.
 */
@Component
@RequiredArgsConstructor
public class ExecuteProblemSql {

    private final ProblemExecutionPort problemExecutionPort;

    /**
     * 문제 SQL을 실행한다.
     *
     * @param input 문제 SQL 실행 입력
     * @return 문제 SQL 실행 결과
     */
    public ProblemExecutionOutput execute(ProblemExecutionInput input) {
        return problemExecutionPort.execute(input);
    }
}
