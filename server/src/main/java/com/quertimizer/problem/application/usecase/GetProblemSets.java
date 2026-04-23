package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.result.ProblemSetSummaryResult;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetProblemSets {

    private final ProblemService problemService;

    public List<ProblemSetSummaryResult> execute(Authentication authentication) {
        return problemService.getProblemSets(resolveAuthenticatedEmail(authentication));
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }

}
