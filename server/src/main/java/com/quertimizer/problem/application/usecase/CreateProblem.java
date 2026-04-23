package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.input.ProblemCreateInput;
import com.quertimizer.problem.application.result.ProblemCreateResult;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateProblem {

    private final ProblemService problemService;

    public ProblemCreateResult execute(ProblemCreateInput input, Authentication authentication) {
        return problemService.createProblem(input, resolveAuthenticatedEmail(authentication));
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }

}
