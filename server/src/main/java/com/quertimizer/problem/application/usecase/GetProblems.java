package com.quertimizer.problem.application.usecase;

import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.problem.application.result.ProblemPageResult;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetProblems {

    private final ProblemService problemService;
    private final AuthService authService;

    public ProblemPageResult execute(int page,
                                     String query,
                                     String dbms,
                                     String solveState,
                                     String solvedCountSort,
                                     String totalSubmitSort,
                                     String successSubmitSort,
                                     String spreadRateSort,
                                     Double spreadRateMin,
                                     Double spreadRateMax,
                                     Authentication authentication) {
        return problemService.getProblems(
                page,
                query,
                dbms,
                solveState,
                resolveCurrentHandle(authentication),
                solvedCountSort,
                totalSubmitSort,
                successSubmitSort,
                spreadRateSort,
                spreadRateMin,
                spreadRateMax
        );
    }

    private String resolveCurrentHandle(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.resolveCurrentHandle(authentication.getName());
    }

}
