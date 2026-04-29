package com.quertimizer.problem.presentation.controller;

import com.quertimizer.auth.presentation.support.AuthSupport;
import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.application.input.ProblemOutputPreviewInput;
import com.quertimizer.judge.application.usecase.BuildProblemOutputPreview;
import com.quertimizer.problem.application.input.CreateProblemInput;
import com.quertimizer.problem.application.input.ProblemOptionsInput;
import com.quertimizer.problem.application.input.ProblemSetAccessInput;
import com.quertimizer.problem.application.usecase.CreateProblem;
import com.quertimizer.problem.application.usecase.GetProblem;
import com.quertimizer.problem.application.usecase.GetProblemOptions;
import com.quertimizer.problem.application.usecase.GetProblemSet;
import com.quertimizer.problem.application.usecase.GetProblemSets;
import com.quertimizer.problem.application.usecase.GetProblems;
import com.quertimizer.problem.presentation.dto.request.ProblemCreateReq;
import com.quertimizer.problem.presentation.dto.request.ProblemOutputPreviewReq;
import com.quertimizer.problem.presentation.dto.request.ProblemSearchReq;
import com.quertimizer.problem.presentation.dto.response.AdminProblemOptionRes;
import com.quertimizer.problem.presentation.dto.response.ProblemCreateRes;
import com.quertimizer.problem.presentation.dto.response.ProblemDetailRes;
import com.quertimizer.problem.presentation.dto.response.ProblemOutputPreviewRes;
import com.quertimizer.problem.presentation.dto.response.ProblemPageRes;
import com.quertimizer.problem.presentation.dto.response.ProblemSetDetailRes;
import com.quertimizer.problem.presentation.dto.response.ProblemSetSummaryRes;
import com.quertimizer.problem.presentation.support.ProblemSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final BuildProblemOutputPreview buildProblemOutputPreview;

    private final ProblemSupport problemSupport;
    private final AuthSupport authSupport;

    /**
     * 문제 목록 필터와 정렬 조건에 맞는 문제 페이지를 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>문제 목록 페이지 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/problems")
    public ResponseEntity<ProblemPageRes> getProblems(@Valid ProblemSearchReq request, Authentication authentication) {
        String currentHandle = problemSupport.resolveCurrentHandle(authentication);

        return ResponseEntity.ok(ProblemPageRes.from(getProblems.execute(
                request.toInput(currentHandle)
        )));
    }

    /**
     * 문제 번호가 일치하는 문제 상세를 반환한다.
     *
     * @param problemId 조회할 문제 번호
     */
    @GetMapping("/problems/{problemId}")
    public ResponseEntity<ProblemDetailRes> getProblem(@PathVariable String problemId) {
        return ResponseEntity.of(getProblem.execute(problemId).map(ProblemDetailRes::from));
    }

    /**
     * 관리자 문제 테이블셋 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 이메일 확인
     *   <li>문제 테이블셋 목록 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/admin/problem-sets")
    public ResponseEntity<List<ProblemSetSummaryRes>> getProblemSets(Authentication authentication) {
        String authenticatedEmail = problemSupport.resolveAuthenticatedEmail(authentication);

        return ResponseEntity.ok(getProblemSets.execute(authenticatedEmail).stream()
                .map(ProblemSetSummaryRes::from)
                .toList());
    }

    /**
     * 관리자 문제 테이블셋 상세를 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 이메일 확인
     *   <li>문제 테이블셋 상세 응답 생성
     * </ol>
     *
     * @param problemSetId 조회할 문제 테이블셋 번호
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/admin/problem-sets/{problemSetId}")
    public ResponseEntity<ProblemSetDetailRes> getProblemSet(@PathVariable String problemSetId,
                                                             Authentication authentication) {
        String authenticatedEmail = problemSupport.resolveAuthenticatedEmail(authentication);

        ProblemSetAccessInput input = new ProblemSetAccessInput(problemSetId, authenticatedEmail);
        return ResponseEntity.of(getProblemSet.execute(input).map(ProblemSetDetailRes::from));
    }

    /**
     * 문제 테이블셋에 연결할 수 있는 관리자 문제 옵션 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 이메일 확인
     *   <li>문제 옵션 목록 응답 생성
     * </ol>
     *
     * @param problemSetId 옵션을 조회할 문제 테이블셋 번호
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/admin/problem-sets/{problemSetId}/problems")
    public ResponseEntity<List<AdminProblemOptionRes>> getProblemOptions(@PathVariable String problemSetId,
                                                                         Authentication authentication) {
        String authenticatedEmail = problemSupport.resolveAuthenticatedEmail(authentication);

        ProblemOptionsInput input = new ProblemOptionsInput(problemSetId, authenticatedEmail);
        return ResponseEntity.ok(getProblemOptions.execute(input).stream()
                .map(AdminProblemOptionRes::from)
                .toList());
    }

    /**
     * 관리자 문제 생성 요청을 저장하고 생성된 문제 Location을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 이메일 확인
     *   <li>문제 생성 응답 생성
     *   <li>생성된 문제 Location 응답 생성
     * </ol>
     *
     * @param request 생성할 문제 요청
     * @param authentication 현재 요청의 인증 정보
     */
    @PostMapping("/admin/problems")
    public ResponseEntity<ProblemCreateRes> createProblem(@Valid @RequestBody ProblemCreateReq request,
                                                          Authentication authentication) {
        String authenticatedEmail = problemSupport.resolveAuthenticatedEmail(authentication);

        CreateProblemInput input = new CreateProblemInput(request.toProblemCreateInput(), authenticatedEmail);
        ProblemCreateRes response = ProblemCreateRes.from(createProblem.execute(input));

        return ResponseEntity.created(URI.create("/problems/" + response.getProblemId())).body(response);
    }

    /**
     * 문제 생성용 출력 예시를 judge 임시 실행 환경에서 생성한다.
     *
     * <ol>
     *   <li>출력 예시 입력 생성
     *   <li>출력 예시 응답 생성
     * </ol>
     *
     * @param request 출력 예시 생성을 위한 SQL 요청
     */
    @PostMapping("/admin/problems/output-preview")
    public ResponseEntity<ProblemOutputPreviewRes> previewProblemOutput(@Valid @RequestBody ProblemOutputPreviewReq request,
                                                                        Authentication authentication,
                                                                        HttpServletRequest httpRequest) {
        ProblemOutputPreviewInput input = new ProblemOutputPreviewInput(
                DbmsType.fromValueOrDefault(request.getDbms(), DbmsType.POSTGRESQL),
                request.getDdl(),
                request.getSampleDataSql(),
                request.getAnswerSql(),
                authentication.getName(),
                authSupport.resolveClientIp(httpRequest)
        );

        return ResponseEntity.ok(ProblemOutputPreviewRes.from(buildProblemOutputPreview.execute(input)));
    }
}
