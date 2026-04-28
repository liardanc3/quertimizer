package com.quertimizer.judge.presentation.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.auth.domain.model.AuthFailReason;
import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.application.input.InteractiveSqlInput;
import com.quertimizer.judge.application.input.SubmitProblemSqlInput;
import com.quertimizer.judge.application.service.JudgeQueryService;
import com.quertimizer.judge.application.service.JudgeWorkspaceService;
import com.quertimizer.judge.application.usecase.CancelInteractiveExecution;
import com.quertimizer.judge.application.usecase.ExecuteInteractiveSql;
import com.quertimizer.judge.application.usecase.SubmitProblemSql;
import com.quertimizer.global.realtime.router.SessionSocketMessage;
import com.quertimizer.global.realtime.router.SessionSocketMessageHandler;
import com.quertimizer.global.realtime.sender.SessionSocketSender;
import com.quertimizer.problem.domain.model.ProblemLogMessage;
import com.quertimizer.problem.domain.model.ProblemQueryResultText;
import com.quertimizer.problem.presentation.realtime.dto.ProblemExecuteRes;
import com.quertimizer.problem.presentation.realtime.dto.ProblemSocketReq;
import com.quertimizer.problem.presentation.realtime.dto.ProblemSubmitProgressRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeSocketMessageHandler implements SessionSocketMessageHandler {

    private static final String PROBLEM_EXECUTE = "problem.execute";
    private static final String PROBLEM_EXECUTE_PAGE = "problem.execute.page";
    private static final String PROBLEM_EXECUTE_STOP = "problem.execute.stop";
    private static final String PROBLEM_SUBMIT = "problem.submit";
    private static final String PROBLEM_LEAVE = "problem.leave";
    private static final String JUDGE_EXECUTE = "judge.execute";
    private static final String JUDGE_EXECUTE_PAGE = "judge.execute.page";
    private static final String JUDGE_EXECUTE_STOP = "judge.execute.stop";
    private static final String JUDGE_SUBMIT = "judge.submit";
    private static final String JUDGE_LEAVE = "judge.leave";

    private final ObjectMapper objectMapper;
    private final ExecuteInteractiveSql executeInteractiveSql;
    private final SubmitProblemSql submitProblemSql;
    private final CancelInteractiveExecution cancelInteractiveExecution;
    private final JudgeWorkspaceService judgeWorkspaceService;
    private final SessionSocketSender sessionSocketSender;
    @Qualifier("problemExecutingExecutor")
    private final TaskExecutor problemExecutingExecutor;

    /**
     * judge WebSocket message type을 이 handler가 처리할 수 있는지 확인한다.
     *
     * @param type 수신한 WebSocket message type
     */
    @Override
    public boolean supports(String type) {
        return PROBLEM_EXECUTE.equals(type)
                || PROBLEM_EXECUTE_PAGE.equals(type)
                || PROBLEM_EXECUTE_STOP.equals(type)
                || PROBLEM_SUBMIT.equals(type)
                || PROBLEM_LEAVE.equals(type)
                || JUDGE_EXECUTE.equals(type)
                || JUDGE_EXECUTE_PAGE.equals(type)
                || JUDGE_EXECUTE_STOP.equals(type)
                || JUDGE_SUBMIT.equals(type)
                || JUDGE_LEAVE.equals(type);
    }

    /**
     * judge WebSocket 메시지를 요청 DTO로 변환하고 type별 처리 흐름으로 라우팅한다.
     *
     * <ol>
     *   <li>payload를 문제 소켓 요청으로 변환
     *   <li>요청 type 검증
     *   <li>type별 실행, 제출, 취소, 이탈 처리로 위임
     * </ol>
     *
     * @param session 수신한 WebSocket 세션
     * @param message 라우팅할 WebSocket 메시지
     * @throws Exception payload 변환 또는 이탈 응답 전송에 실패한 경우
     */
    @Override
    public void handle(WebSocketSession session, SessionSocketMessage message) throws Exception {
        ProblemSocketReq request = objectMapper.treeToValue(message.payload(), ProblemSocketReq.class);
        if (request.type() == null || request.type().isBlank()) {
            return;
        }

        switch (request.type()) {
            case PROBLEM_EXECUTE, JUDGE_EXECUTE -> handleProblemExecute(session, request);
            case PROBLEM_EXECUTE_PAGE, JUDGE_EXECUTE_PAGE -> handleProblemExecutePage(session, request);
            case PROBLEM_EXECUTE_STOP, JUDGE_EXECUTE_STOP -> handleProblemExecuteStop(session);
            case PROBLEM_SUBMIT, JUDGE_SUBMIT -> handleProblemSubmit(session, request);
            case PROBLEM_LEAVE, JUDGE_LEAVE -> handleProblemLeave(session, request);
            default -> {
            }
        }
    }

    /**
     * judge WebSocket 연결 종료 시 작업용 스키마 정리를 예약한다.
     *
     * @param session 종료된 WebSocket 세션
     * @param status 연결 종료 상태
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        judgeWorkspaceService.handleConnectionClose(session.getId());
    }

    private void handleProblemExecute(WebSocketSession session, ProblemSocketReq request) {
        // SQL 실행 요청을 비동기로 처리
        String authenticatedHandle = resolveAuthenticatedHandle(session);
        problemExecutingExecutor.execute(() -> executeProblemQuery(session, request, authenticatedHandle));
    }

    private void handleProblemExecutePage(WebSocketSession session, ProblemSocketReq request) {
        // SQL 실행 결과 페이지 요청을 비동기로 처리
        String authenticatedHandle = resolveAuthenticatedHandle(session);
        problemExecutingExecutor.execute(() -> executeProblemQueryPage(session, request, authenticatedHandle));
    }

    private void handleProblemSubmit(WebSocketSession session, ProblemSocketReq request) {
        // 제출 요청을 비동기로 처리
        String authenticatedHandle = resolveAuthenticatedHandle(session);
        problemExecutingExecutor.execute(() -> submitProblemQuery(session, request, authenticatedHandle));
    }

    private void handleProblemExecuteStop(WebSocketSession session) {
        // 진행 중인 인터랙티브 실행을 취소
        cancelInteractiveExecution.execute(session.getId());
    }

    private void handleProblemLeave(WebSocketSession session, ProblemSocketReq request) throws Exception {
        // 명시적 이탈 시 작업용 스키마를 즉시 정리
        judgeWorkspaceService.handleExplicitLeave(session.getId());
        sessionSocketSender.sendObjectMessage(session, ProblemExecuteRes.leaveSuccess(request.problemId()));
    }

    private void executeProblemQuery(WebSocketSession session, ProblemSocketReq request, String authenticatedHandle) {
        // 실행 결과를 소켓 응답으로 변환
        try {
            JudgeQueryService.QueryExecutionResult executionResult = executeInteractiveSqlRequest(session, request, authenticatedHandle);

            sessionSocketSender.sendObjectMessage(session, ProblemExecuteRes.executionSuccess(
                    executionResult.problemId(),
                    executionResult.mode(),
                    executionResult.message(),
                    executionResult.columns(),
                    executionResult.rows(),
                    executionResult.planLines(),
                    executionResult.rowCount(),
                    executionResult.currentPage(),
                    executionResult.pageSize(),
                    executionResult.executionTimeMs(),
                    executionResult.cost()
            ));
        } catch (Exception exception) {
            sendExecutionFailure(session, exception, ProblemLogMessage.EXECUTE_FAILURE_RESPONSE_SEND_FAILED);
        }
    }

    private void executeProblemQueryPage(WebSocketSession session, ProblemSocketReq request, String authenticatedHandle) {
        // 실행 페이지 결과를 소켓 응답으로 변환
        try {
            JudgeQueryService.QueryExecutionResult executionResult = executeInteractiveSqlRequest(session, request, authenticatedHandle);

            sessionSocketSender.sendObjectMessage(session, ProblemExecuteRes.executionSuccess(
                    executionResult.problemId(),
                    executionResult.mode(),
                    executionResult.message(),
                    executionResult.columns(),
                    executionResult.rows(),
                    executionResult.planLines(),
                    executionResult.rowCount(),
                    executionResult.currentPage(),
                    executionResult.pageSize(),
                    executionResult.executionTimeMs(),
                    executionResult.cost()
            ));
        } catch (Exception exception) {
            sendExecutionFailure(session, exception, ProblemLogMessage.EXECUTE_PAGE_RESPONSE_SEND_FAILED);
        }
    }

    private JudgeQueryService.QueryExecutionResult executeInteractiveSqlRequest(WebSocketSession session,
                                                                                ProblemSocketReq request,
                                                                                String authenticatedHandle) {
        // 소켓 요청을 인터랙티브 SQL 실행 입력으로 변환
        return executeInteractiveSql.execute(new InteractiveSqlInput(
                authenticatedHandle,
                session.getId(),
                request.problemId(),
                request.sql(),
                resolveDbmsType(request.dbms()),
                request.page(),
                request.pageSize()
        ));
    }

    private void submitProblemQuery(WebSocketSession session, ProblemSocketReq request, String authenticatedHandle) {
        // 제출 결과와 progress를 소켓 응답으로 변환
        try {
            SubmitProblemSqlInput input = new SubmitProblemSqlInput(
                    authenticatedHandle,
                    session.getId(),
                    request.problemId(),
                    request.sql(),
                    resolveDbmsType(request.dbms()),
                    progress -> sendProblemSubmitProgressMessage(session, progress)
            );
            JudgeQueryService.ProblemSubmitResult submitResult = submitProblemSql.execute(input);

            ProblemExecuteRes response = submitResult.success()
                    ? ProblemExecuteRes.submitSuccess(
                            submitResult.problemId(),
                            submitResult.message(),
                            submitResult.executionTimeMs()
                    )
                    : ProblemExecuteRes.submitFailure(
                            submitResult.problemId(),
                            submitResult.message()
                    );
            sessionSocketSender.sendObjectMessage(session, response);
        } catch (Exception exception) {
            try {
                sessionSocketSender.sendObjectMessage(session, ProblemExecuteRes.submitFailure(request.problemId(), resolveErrorMessage(exception)));
            } catch (Exception sendException) {
                log.warn(ProblemLogMessage.SUBMIT_FAILURE_RESPONSE_SEND_FAILED.getMessage(), sendException);
            }
        }
    }

    private void sendProblemSubmitProgressMessage(WebSocketSession session, JudgeQueryService.ProblemSubmitProgress progress) {
        // 제출 progress를 기존 하위 호환 message type으로 전송
        try {
            sessionSocketSender.sendObjectMessage(session, ProblemSubmitProgressRes.of(
                    progress.problemId(),
                    progress.stepKey(),
                    progress.status(),
                    progress.message(),
                    progress.detailLines(),
                    progress.statementKey(),
                    progress.statementIndex(),
                    progress.statementSql(),
                    progress.statementMode(),
                    progress.statementReference()
            ));
        } catch (Exception exception) {
            log.warn(ProblemLogMessage.SUBMIT_PROGRESS_RESPONSE_SEND_FAILED.getMessage(), exception);
        }
    }

    private void sendExecutionFailure(WebSocketSession session,
                                      Exception exception,
                                      ProblemLogMessage logMessage) {
        try {
            sessionSocketSender.sendObjectMessage(session, ProblemExecuteRes.error(resolveErrorMessage(exception)));
        } catch (Exception sendException) {
            log.warn(logMessage.getMessage(), sendException);
        }
    }

    private String resolveAuthenticatedHandle(WebSocketSession session) {
        // 소켓 세션에서 인증 handle을 확인
        String handle = (String) session.getAttributes().get("handle");
        if (handle == null || handle.isBlank()) {
            throw new IllegalArgumentException(AuthFailReason.LOGIN_INFORMATION_NOT_FOUND.getMessage());
        }

        return handle;
    }

    private DbmsType resolveDbmsType(String dbms) {
        // DBMS 유형을 해석
        return DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL);
    }

    private String resolveErrorMessage(Exception exception) {
        // 소켓 error message를 결정
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }

        return ProblemQueryResultText.PROBLEM_EXECUTION_FAILED.getText();
    }
}
