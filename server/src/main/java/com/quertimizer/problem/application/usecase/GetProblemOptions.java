package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.input.ProblemOptionsInput;
import com.quertimizer.problem.application.output.AdminProblemOptionOutput;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetProblemOptions {

    private final ProblemService problemService;

    /**
     * 문제 관리용 문제 옵션 목록을 조회한다.
     *
     * @param input 옵션을 조회할 문제 테이블셋과 인증 이메일 입력
     */
    public List<AdminProblemOptionOutput> execute(ProblemOptionsInput input) {
        return problemService.getProblemOptions(input.getProblemSetId(), input.getAuthenticatedEmail());
    }
}
