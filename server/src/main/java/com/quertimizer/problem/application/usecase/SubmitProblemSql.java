package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.input.ProblemSubmissionInput;
import com.quertimizer.problem.application.output.ProblemSubmissionOutput;
import com.quertimizer.problem.application.port.ProblemSubmissionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 문제 SQL 제출 요청을 제출 포트에 위임한다.
 */
@Component
@RequiredArgsConstructor
public class SubmitProblemSql {

    private final ProblemSubmissionPort problemSubmissionPort;

    /**
     * 문제 SQL을 제출하고 채점한다.
     *
     * @param input 문제 SQL 제출 입력
     * @return 문제 SQL 제출 결과
     */
    public ProblemSubmissionOutput execute(ProblemSubmissionInput input) {
        return problemSubmissionPort.submit(input);
    }
}
