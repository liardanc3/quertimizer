package com.quertimizer.problem.presentation.controller;

import com.quertimizer.problem.application.usecase.CreateProblem;
import com.quertimizer.problem.application.usecase.GetProblemOptions;
import com.quertimizer.problem.application.usecase.GetProblemSet;
import com.quertimizer.problem.application.usecase.GetProblemSets;
import com.quertimizer.problem.application.usecase.GetProblem;
import com.quertimizer.problem.application.usecase.GetProblems;
import com.quertimizer.problem.presentation.dto.request.ProblemCreateReq;
import com.quertimizer.problem.presentation.dto.response.AdminProblemOptionRes;
import com.quertimizer.problem.presentation.dto.response.ProblemCreateRes;
import com.quertimizer.problem.presentation.dto.response.ProblemDetailRes;
import com.quertimizer.problem.presentation.dto.response.ProblemPageRes;
import com.quertimizer.problem.presentation.dto.response.ProblemSetDetailRes;
import com.quertimizer.problem.presentation.dto.response.ProblemSetSummaryRes;
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

    private final GetProblems getProblems;
    private final GetProblem getProblem;
    private final GetProblemSets getProblemSets;
    private final GetProblemSet getProblemSet;
    private final GetProblemOptions getProblemOptions;
    private final CreateProblem createProblem;

    @GetMapping("/problems")
    public ResponseEntity<ProblemPageRes> getProblems(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(required = false) String query,
                                                      @RequestParam(defaultValue = "postgresql") String dbms,
                                                      @RequestParam(defaultValue = "all") String solveState,
                                                      @RequestParam(defaultValue = "desc") String solvedCountSort,
                                                      @RequestParam(defaultValue = "none") String totalSubmitSort,
                                                      @RequestParam(defaultValue = "none") String successSubmitSort,
                                                      @RequestParam(defaultValue = "none") String spreadRateSort,
                                                      @RequestParam(required = false) Double spreadRateMin,
                                                      @RequestParam(required = false) Double spreadRateMax,
                                                      Authentication authentication) {

        return ResponseEntity.ok(ProblemPageRes.from(getProblems.execute(
                page,
                query,
                dbms,
                solveState,
                solvedCountSort,
                totalSubmitSort,
                successSubmitSort,
                spreadRateSort,
                spreadRateMin,
                spreadRateMax,
                authentication
        )));
    }

    @GetMapping("/problems/{problemId}")
    public ResponseEntity<ProblemDetailRes> getProblem(@PathVariable String problemId) {
        return ResponseEntity.of(getProblem.execute(problemId).map(ProblemDetailRes::from));
    }

    @GetMapping("/admin/problem-sets")
    public ResponseEntity<List<ProblemSetSummaryRes>> getProblemSets(Authentication authentication) {
        return ResponseEntity.ok(getProblemSets.execute(authentication).stream()
                .map(ProblemSetSummaryRes::from)
                .toList());
    }

    @GetMapping("/admin/problem-sets/{problemSetId}")
    public ResponseEntity<ProblemSetDetailRes> getProblemSet(@PathVariable String problemSetId,
                                                             Authentication authentication) {

        return ResponseEntity.of(getProblemSet.execute(problemSetId, authentication).map(ProblemSetDetailRes::from));
    }

    @GetMapping("/admin/problem-sets/{problemSetId}/problems")
    public ResponseEntity<List<AdminProblemOptionRes>> getProblemOptions(@PathVariable String problemSetId,
                                                                         Authentication authentication) {

        return ResponseEntity.ok(getProblemOptions.execute(problemSetId, authentication).stream()
                .map(AdminProblemOptionRes::from)
                .toList());
    }

    @PostMapping("/admin/problems")
    public ResponseEntity<ProblemCreateRes> createProblem(@Valid @RequestBody ProblemCreateReq request,
                                                          Authentication authentication) {

        ProblemCreateRes response = ProblemCreateRes.from(createProblem.execute(request.toProblemCreateInput(), authentication));

        return ResponseEntity.created(URI.create("/problems/" + response.getProblemId())).body(response);
    }
}
