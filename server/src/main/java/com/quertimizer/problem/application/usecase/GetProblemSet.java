package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.result.ProblemSetDetailResult;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetProblemSet {

    private final ProblemService problemService;

    public Optional<ProblemSetDetailResult> execute(String problemSetId, Authentication authentication) {
        return problemService.getProblemSet(problemSetId, resolveAuthenticatedEmail(authentication));
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }

}
