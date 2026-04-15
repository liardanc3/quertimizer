package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.request.ProblemCreateReq;
import com.quertimizer.endpoint.api.dto.response.ProblemCreateRes;
import com.quertimizer.endpoint.api.dto.response.ProblemDetailRes;
import com.quertimizer.endpoint.api.dto.response.ProblemPageRes;
import com.quertimizer.endpoint.api.dto.response.ProblemSetDetailRes;
import com.quertimizer.endpoint.api.dto.response.ProblemSetSummaryRes;
import com.quertimizer.service.ProblemService;
import com.quertimizer.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;
    private final UserAccountService userAccountService;

    @GetMapping("/problems")
    public ResponseEntity<ProblemPageRes> getProblems(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(required = false) String query,
                                                      @RequestParam(defaultValue = "all") String solveState,
                                                      @RequestParam(defaultValue = "desc") String solvedCountSort,
                                                      @RequestParam(defaultValue = "none") String spreadRateSort,
                                                      @RequestParam(required = false) Double spreadRateMin,
                                                      @RequestParam(required = false) Double spreadRateMax,
                                                      Authentication authentication) {

        // 현재 조회 조건 기준 문제 목록 페이지 반환
        return ResponseEntity.ok(problemService.getProblems(
                page,
                query,
                solveState,
                resolveCurrentUserId(authentication),
                solvedCountSort,
                spreadRateSort,
                spreadRateMin,
                spreadRateMax
        ));
    }

    @GetMapping("/problems/{problemId}")
    public ResponseEntity<ProblemDetailRes> getProblem(@PathVariable String problemId) {

        // 문제 id 기준 상세 응답 반환
        return ResponseEntity.of(problemService.getProblem(problemId));
    }

    @GetMapping("/admin/problem-sets")
    public ResponseEntity<List<ProblemSetSummaryRes>> getProblemSets() {

        // 기존 테이블셋 목록 반환
        return ResponseEntity.ok(problemService.getProblemSets());
    }

    @GetMapping("/admin/problem-sets/{problemSetId}")
    public ResponseEntity<ProblemSetDetailRes> getProblemSet(@PathVariable String problemSetId) {

        // 테이블셋 번호 기준 상세 반환
        return ResponseEntity.of(problemService.getProblemSet(problemSetId));
    }

    @PostMapping("/admin/problems")
    public ResponseEntity<ProblemCreateRes> createProblem(@Valid @RequestBody ProblemCreateReq request) {

        // 관리자 문제 생성 처리
        ProblemCreateRes response = problemService.createProblem(request);

        return ResponseEntity.created(URI.create("/problems/" + response.getProblemId())).body(response);
    }

    private String resolveCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return userAccountService.resolveCurrentUserId(authentication.getName());
    }

}
