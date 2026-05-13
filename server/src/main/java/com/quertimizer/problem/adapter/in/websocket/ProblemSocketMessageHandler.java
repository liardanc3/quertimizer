package com.quertimizer.problem.adapter.in.websocket;

import com.quertimizer.global.websocket.sender.WebSocketSender;
import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.input.ProblemSubmissionInput;
import com.quertimizer.problem.application.port.in.CancelProblemExecutionUseCase;
import com.quertimizer.problem.application.port.in.CloseProblemExecutionSessionUseCase;
import com.quertimizer.problem.application.port.in.ExecuteProblemSqlUseCase;
import com.quertimizer.problem.application.port.in.SubmitProblemSqlUseCase;
import com.quertimizer.problem.adapter.in.websocket.dto.ProblemExecuteRes;
import com.quertimizer.problem.adapter.in.websocket.dto.ProblemExecutionProgressRes;
import com.quertimizer.problem.adapter.in.websocket.dto.ProblemSocketReq;
import com.quertimizer.problem.adapter.in.websocket.dto.ProblemSubmitProgressRes;
import com.quertimizer.problem.adapter.in.http.support.ProblemSupport;
import com.quertimizer.problem.adapter.in.http.support.ProblemSupport.WebSocketReplyTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
@RequiredArgsConstructor
public class ProblemSocketMessageHandler {

    private final ExecuteProblemSqlUseCase executeProblemSql;
    private final SubmitProblemSqlUseCase submitProblemSql;
    private final CancelProblemExecutionUseCase cancelProblemExecution;
    private final CloseProblemExecutionSessionUseCase closeProblemExecutionSession;
    private final WebSocketSender webSocketSender;
    private final ProblemSupport problemSupport;

    /**
     * SQL 실행 WebSocket 요청을 처리한다.
     *
     * <ol>
     *   <li>응답 대상과 실행 세션 구성
     *   <li>SQL 실행
     *   <li>실행 성공 응답 전송
     * </ol>
     *
     * @param request SQL 실행에 사용하는 WebSocket 요청
     * @param headerAccessor 인증 handle 확인과 응답 전송에 사용하는 WebSocket 헤더
     */
    @MessageMapping({"problem.execute", "judge.execute"})
    public void handleProblemExecute(ProblemSocketReq request, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketReplyTarget replyTarget = problemSupport.createWebSocketReplyTarget(headerAccessor);
        ProblemExecutionInput input = request.toInput(
                replyTarget,
                progress -> sendToSession(replyTarget, ProblemExecutionProgressRes.from(progress))
        );

        ProblemExecuteRes response = ProblemExecuteRes.executionSuccess(executeProblemSql.execute(input));
        sendToSession(replyTarget, response);
    }

    /**
     * SQL 실행 페이지 WebSocket 요청을 처리한다.
     *
     * <ol>
     *   <li>응답 대상과 실행 세션 구성
     *   <li>SQL 실행 페이지 조회
     *   <li>실행 페이지 응답 전송
     * </ol>
     *
     * @param request SQL 실행 페이지 조회에 사용하는 WebSocket 요청
     * @param headerAccessor 인증 handle 확인과 응답 전송에 사용하는 WebSocket 헤더
     */
    @MessageMapping({"problem.execute.page", "judge.execute.page"})
    public void handleProblemExecutePage(ProblemSocketReq request, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketReplyTarget replyTarget = problemSupport.createWebSocketReplyTarget(headerAccessor);
        ProblemExecutionInput input = request.toInput(
                replyTarget,
                progress -> sendToSession(replyTarget, ProblemExecutionProgressRes.from(progress))
        );

        ProblemExecuteRes response = ProblemExecuteRes.executionSuccess(executeProblemSql.execute(input));
        sendToSession(replyTarget, response);
    }

    /**
     * SQL 제출 WebSocket 요청을 처리한다.
     *
     * <ol>
     *   <li>응답 대상과 실행 세션 구성
     *   <li>SQL 제출
     *   <li>제출 결과 응답 전송
     * </ol>
     *
     * @param request SQL 제출에 사용하는 WebSocket 요청
     * @param headerAccessor 인증 handle 확인과 응답 전송에 사용하는 WebSocket 헤더
     */
    @MessageMapping({"problem.submit", "judge.submit"})
    public void handleProblemSubmit(ProblemSocketReq request, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketReplyTarget replyTarget = problemSupport.createWebSocketReplyTarget(headerAccessor);
        ProblemSubmissionInput input = request.toSubmissionInput(
                replyTarget,
                progress -> sendToSession(replyTarget, ProblemSubmitProgressRes.from(progress))
        );

        ProblemExecuteRes response = ProblemExecuteRes.submitResult(submitProblemSql.execute(input));
        sendToSession(replyTarget, response);
    }

    /**
     * 진행 중인 SQL 실행 취소 WebSocket 요청을 처리한다.
     *
     * @param request 취소 요청 페이로드
     * @param headerAccessor 취소 대상 실행 세션을 식별하는 WebSocket 헤더
     */
    @MessageMapping({"problem.execute.stop", "judge.execute.stop"})
    public void handleProblemExecuteStop(ProblemSocketReq request, SimpMessageHeaderAccessor headerAccessor) {
        String executionSessionId = problemSupport.createExecutionSessionId(problemSupport.resolveWebSocketSessionId(headerAccessor));

        cancelProblemExecution.execute(executionSessionId);
    }

    /**
     * 문제 풀이 화면 이탈 WebSocket 요청을 처리한다.
     *
     * <ol>
     *   <li>judge 실행 환경 정리
     *   <li>이탈 성공 응답 전송
     * </ol>
     *
     * @param request 이탈 대상 문제 ID를 포함한 WebSocket 요청
     * @param headerAccessor 정리 대상 실행 세션 식별과 응답 전송에 사용하는 WebSocket 헤더
     */
    @MessageMapping({"problem.leave", "judge.leave"})
    public void handleProblemLeave(ProblemSocketReq request, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketReplyTarget replyTarget = problemSupport.createWebSocketReplyTarget(headerAccessor);

        closeProblemExecutionSession.execute(replyTarget.getExecutionSessionId());
        sendToSession(replyTarget, ProblemExecuteRes.leaveSuccess(request.problemId()));
    }

    /**
     * WebSocket 연결 종료 시 judge 실행 세션을 정리한다.
     *
     * <ol>
     *   <li>WebSocket 세션 ID 확인
     *   <li>judge 실행 환경 정리
     * </ol>
     *
     * @param event 종료된 WebSocket 세션 이벤트
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        closeProblemExecutionSession.execute(problemSupport.createExecutionSessionId(sessionId));
    }

    private void sendToSession(WebSocketReplyTarget replyTarget, Object payload) {
        // WebSocket 응답 전송
        webSocketSender.sendToSessionUnchecked(replyTarget.getHandle(), replyTarget.getReplySessionId(), payload);
    }

}
