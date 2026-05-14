package com.quertimizer.problem.adapter.in.http;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.log.LogMdcContext;
import com.quertimizer.global.util.ClientIpResolver;
import com.quertimizer.global.websocket.sender.WebSocketSender;
import com.quertimizer.problem.application.output.ProblemCreateOutput;
import com.quertimizer.problem.application.output.ProblemExamplePreviewOutput;
import com.quertimizer.problem.application.output.ProblemOutputPreviewOutput;
import com.quertimizer.problem.application.port.in.CreateProblemUseCase;
import com.quertimizer.problem.application.port.in.GetProblemUseCase;
import com.quertimizer.problem.application.port.in.GetProblemOptionsUseCase;
import com.quertimizer.problem.application.port.in.GetProblemSetUseCase;
import com.quertimizer.problem.application.port.in.GetProblemSetsUseCase;
import com.quertimizer.problem.application.port.in.GetProblemsUseCase;
import com.quertimizer.problem.application.port.in.PreviewProblemExamplesUseCase;
import com.quertimizer.problem.application.port.in.PreviewProblemUseCase;
import com.quertimizer.problem.adapter.in.http.request.ProblemCreateReq;
import com.quertimizer.problem.adapter.in.http.request.ProblemExamplePreviewReq;
import com.quertimizer.problem.adapter.in.http.request.ProblemOutputPreviewReq;
import com.quertimizer.problem.adapter.in.http.request.ProblemSearchReq;
import com.quertimizer.problem.adapter.in.websocket.dto.ProblemCreateProgressRes;
import com.quertimizer.problem.adapter.in.http.response.AdminProblemOptionRes;
import com.quertimizer.problem.adapter.in.http.response.ProblemDetailRes;
import com.quertimizer.problem.adapter.in.http.response.ProblemExamplePreviewRes;
import com.quertimizer.problem.adapter.in.http.response.ProblemResultPreviewRes;
import com.quertimizer.problem.adapter.in.http.response.ProblemPageRes;
import com.quertimizer.problem.adapter.in.http.response.ProblemSetDetailRes;
import com.quertimizer.problem.adapter.in.http.response.ProblemSetSummaryRes;
import com.quertimizer.problem.adapter.in.http.support.ProblemSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_CREATE_FAILED;
import static com.quertimizer.problem.domain.model.ProblemLogMessage.CREATE_PROGRESS_RESPONSE_SEND_FAILED;

@RestController
@Slf4j
public class ProblemController {

    private final GetProblemsUseCase getProblems;
    private final GetProblemUseCase getProblem;
    private final GetProblemSetsUseCase getProblemSets;
    private final GetProblemSetUseCase getProblemSet;
    private final GetProblemOptionsUseCase getProblemOptions;
    private final CreateProblemUseCase createProblem;
    private final PreviewProblemUseCase previewProblem;
    private final PreviewProblemExamplesUseCase previewProblemExamples;

    private final ProblemSupport problemSupport;
    private final ClientIpResolver clientIpResolver;
    private final WebSocketSender webSocketSender;
    private final TaskExecutor taskExecutor;

    public ProblemController(GetProblemsUseCase getProblems, GetProblemUseCase getProblem,
                             GetProblemSetsUseCase getProblemSets, GetProblemSetUseCase getProblemSet,
                             GetProblemOptionsUseCase getProblemOptions, CreateProblemUseCase createProblem,
                             PreviewProblemUseCase previewProblem, PreviewProblemExamplesUseCase previewProblemExamples,
                             ProblemSupport problemSupport, ClientIpResolver clientIpResolver,
                             WebSocketSender webSocketSender,
                             @Qualifier("problemCreateTaskExecutor") TaskExecutor taskExecutor) {
        this.getProblems = getProblems;
        this.getProblem = getProblem;
        this.getProblemSets = getProblemSets;
        this.getProblemSet = getProblemSet;
        this.getProblemOptions = getProblemOptions;
        this.createProblem = createProblem;
        this.previewProblem = previewProblem;
        this.previewProblemExamples = previewProblemExamples;
        this.problemSupport = problemSupport;
        this.clientIpResolver = clientIpResolver;
        this.webSocketSender = webSocketSender;
        this.taskExecutor = taskExecutor;
    }

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

        return ResponseEntity.ok(ProblemPageRes.from(getProblems.execute(request.toInput(currentHandle))));
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
     */
    @GetMapping("/admin/problem-sets")
    public ResponseEntity<List<ProblemSetSummaryRes>> getProblemSets() {
        return ResponseEntity.ok(getProblemSets.execute().stream()
                .map(ProblemSetSummaryRes::from)
                .toList());
    }

    /**
     * 관리자 문제 테이블셋 상세를 반환한다.
     *
     * @param problemSetId 조회할 문제 테이블셋 번호
     */
    @GetMapping("/admin/problem-sets/{problemSetId}")
    public ResponseEntity<ProblemSetDetailRes> getProblemSet(@PathVariable String problemSetId) {
        return ResponseEntity.of(getProblemSet.execute(problemSetId).map(ProblemSetDetailRes::from));
    }

    /**
     * 문제 테이블셋에 연결할 수 있는 관리자 문제 옵션 목록을 반환한다.
     *
     * @param problemSetId 옵션을 조회할 문제 테이블셋 번호
     */
    @GetMapping("/admin/problem-sets/{problemSetId}/problems")
    public ResponseEntity<List<AdminProblemOptionRes>> getProblemOptions(@PathVariable String problemSetId) {
        return ResponseEntity.ok(getProblemOptions.execute(problemSetId).stream()
                .map(AdminProblemOptionRes::from)
                .toList());
    }

    /**
     * 관리자 문제 생성을 비동기로 요청한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>문제 생성 작업 비동기 실행
     *   <li>문제 생성 요청 접수 응답 생성
     * </ol>
     *
     * @param request 생성할 문제 요청
     * @param authentication 현재 요청의 인증 정보
     */
    @PostMapping("/admin/problems")
    public ResponseEntity<Void> createProblem(@Valid @RequestBody ProblemCreateReq request, Authentication authentication) {
        String currentHandle = problemSupport.resolveCurrentHandle(authentication);

        taskExecutor.execute(LogMdcContext.wrap(() -> {
            try {
                ProblemCreateOutput output = createProblem.execute(request.toInput(progress -> sendProblemCreateProgress(currentHandle, ProblemCreateProgressRes.from(progress))));
                sendProblemCreateProgress(currentHandle, ProblemCreateProgressRes.completed(output.problemId()));
            } catch (Exception exception) {
                log.error("문제 생성 비동기 작업 실패", exception);
                sendProblemCreateProgress(currentHandle, ProblemCreateProgressRes.failed(resolveProblemCreateFailureMessage(exception)));
            }
        }));

        return ResponseEntity.accepted().build();
    }

    /**
     * 문제 생성용 출력 예시를 judge 경로에서 생성한다.
     *
     * <ol>
     *   <li>출력 예시 입력 생성
     *   <li>출력 예시 응답 생성
     * </ol>
     *
     * @param request 출력 예시 생성을 위한 SQL 요청
     * @param authentication 현재 요청의 인증 정보
     * @param httpRequest 클라이언트 IP 확인에 사용하는 HTTP 요청
     */
    @PostMapping("/admin/problems/output-preview")
    public ResponseEntity<ProblemResultPreviewRes> previewProblem(@Valid @RequestBody ProblemOutputPreviewReq request,
                                                                  Authentication authentication,
                                                                  HttpServletRequest httpRequest) {
        ProblemOutputPreviewOutput problemOutputPreviewOutput =
                previewProblem.execute(request.toInput(authentication.getName(), clientIpResolver.resolve(httpRequest)));
        return ResponseEntity.ok(ProblemResultPreviewRes.from(problemOutputPreviewOutput));
    }

    /**
     * 문제 생성용 데이터 예시와 출력 예시를 judge 경로에서 생성한다.
     *
     * <ol>
     *   <li>예시 입력 생성
     *   <li>예시 응답 생성
     * </ol>
     *
     * @param request 예시 생성을 위한 SQL 요청
     * @param authentication 현재 요청의 인증 정보
     * @param httpRequest 클라이언트 IP 확인에 사용하는 HTTP 요청
     */
    @PostMapping("/admin/problems/example-preview")
    public ResponseEntity<ProblemExamplePreviewRes> previewProblemExamples(@Valid @RequestBody ProblemExamplePreviewReq request,
                                                                          Authentication authentication,
                                                                          HttpServletRequest httpRequest) {
        ProblemExamplePreviewOutput problemExamplePreviewOutput =
                previewProblemExamples.execute(request.toInput(authentication.getName(), clientIpResolver.resolve(httpRequest)));
        return ResponseEntity.ok(ProblemExamplePreviewRes.from(problemExamplePreviewOutput));
    }

    private void sendProblemCreateProgress(String handle, ProblemCreateProgressRes response) {
        // 문제 생성 진행 상태 WebSocket 전송 실패 시 경고 로그 기록
        try {
            webSocketSender.sendToUser(handle, response);
        } catch (Exception exception) {
            log.warn(CREATE_PROGRESS_RESPONSE_SEND_FAILED.getMessage(), exception);
        }
    }

    private String resolveProblemCreateFailureMessage(Exception exception) {
        // 문제 생성 실패 메시지 우선순위에 따라 반환
        if (exception instanceof BusinessException) {
            return exception.getMessage();
        }

        return Optional.ofNullable(exception.getMessage())
                .filter(message -> !message.isBlank())
                .orElse(PROBLEM_CREATE_FAILED.getMessage());
    }
}
