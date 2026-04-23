package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.result.AdminProblemOptionResult;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetProblemOptions {

    private final ProblemService problemService;

    public List<AdminProblemOptionResult> execute(String problemSetId, Authentication authentication) {
        return problemService.getProblemOptions(problemSetId, resolveAuthenticatedEmail(authentication));
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }

}
