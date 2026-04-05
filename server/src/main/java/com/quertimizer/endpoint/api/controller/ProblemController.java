package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.response.ProblemDetailRes;
import com.quertimizer.endpoint.api.dto.response.ProblemPageRes;
import com.quertimizer.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping("/problems")
    public ResponseEntity<ProblemPageRes> getProblems(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(required = false) String query,
                                                      @RequestParam(defaultValue = "all") String solveState,
                                                      @RequestParam(defaultValue = "desc") String solvedCountSort,
                                                      Authentication authentication) {

        // 현재 조회 조건 기준 문제 목록 페이지 반환
        return ResponseEntity.ok(problemService.getProblems(
                page,
                query,
                solveState,
                resolveCurrentUserId(authentication),
                solvedCountSort
        ));
    }

    @GetMapping("/problems/{problemId}")
    public ResponseEntity<ProblemDetailRes> getProblem(@PathVariable String problemId) {

        // 문제 id 기준 상세 응답 반환
        return ResponseEntity.of(problemService.getProblem(problemId));
    }

    private String resolveCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }

}
