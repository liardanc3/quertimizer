package com.quertimizer.problem.presentation.realtime;

import com.quertimizer.global.realtime.sender.SessionStompSender;
import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.input.ProblemSubmissionInput;
import com.quertimizer.problem.application.usecase.CancelProblemExecution;
import com.quertimizer.problem.application.usecase.CloseProblemExecutionSession;
import com.quertimizer.problem.application.usecase.ExecuteProblemSql;
import com.quertimizer.problem.application.usecase.SubmitProblemSql;
import com.quertimizer.problem.presentation.realtime.dto.ProblemExecuteRes;
import com.quertimizer.problem.presentation.realtime.dto.ProblemSocketReq;
import com.quertimizer.problem.presentation.realtime.dto.ProblemSubmitProgressRes;
import com.quertimizer.problem.presentation.support.ProblemSupport;
import com.quertimizer.problem.presentation.support.ProblemSupport.StompReplyTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * 문제 실행/제출 STOMP 메시지를 문제 애플리케이션 흐름으로 연결한다.
 */
@Controller
@RequiredArgsConstructor
public class ProblemSocketMessageHandler {

    private final ExecuteProblemSql executeProblemSql;
    private final SubmitProblemSql submitProblemSql;
    private final CancelProblemExecution cancelProblemExecution;
    private final CloseProblemExecutionSession closeProblemExecutionSession;
    private final SessionStompSender sessionStompSender;
    private final ProblemSupport problemSupport;

    /**
     * 문제 SQL 실행 STOMP 요청을 처리한다.
     *
     * <ol>
     *   <li>응답 대상과 실행 세션 구성
     *   <li>SQL 실행
     *   <li>실행 성공 응답 전송
     * </ol>
     *
     * @param request SQL 실행에 사용하는 STOMP 요청
     * @param headerAccessor 인증 handle 확인과 응답 전송에 사용하는 STOMP 헤더
     */
    @MessageMapping({"problem.execute", "judge.execute"})
    public void handleProblemExecute(ProblemSocketReq request, SimpMessageHeaderAccessor headerAccessor) {
        StompReplyTarget replyTarget = problemSupport.createStompReplyTarget(headerAccessor);
        ProblemExecutionInput input = request.toInput(replyTarget);

        ProblemExecuteRes response = ProblemExecuteRes.executionSuccess(executeProblemSql.execute(input));
        sendToSession(replyTarget, response);
    }

    /**
     * 문제 SQL 실행 페이지 STOMP 요청을 처리한다.
     *
     * <ol>
     *   <li>응답 대상과 실행 세션 구성
     *   <li>SQL 실행 페이지 조회
     *   <li>실행 페이지 응답 전송
     * </ol>
     *
     * @param request SQL 실행 페이지 조회에 사용하는 STOMP 요청
     * @param headerAccessor 인증 handle 확인과 응답 전송에 사용하는 STOMP 헤더
     */
    @MessageMapping({"problem.execute.page", "judge.execute.page"})
    public void handleProblemExecutePage(ProblemSocketReq request, SimpMessageHeaderAccessor headerAccessor) {
        StompReplyTarget replyTarget = problemSupport.createStompReplyTarget(headerAccessor);
        ProblemExecutionInput input = request.toInput(replyTarget);

        ProblemExecuteRes response = ProblemExecuteRes.executionSuccess(executeProblemSql.execute(input));
        sendToSession(replyTarget, response);
    }

    /**
     * 문제 SQL 제출 STOMP 요청을 처리한다.
     *
     * <ol>
     *   <li>응답 대상과 실행 세션 구성
     *   <li>SQL 제출
     *   <li>제출 결과 응답 전송
     * </ol>
     *
     * @param request SQL 제출에 사용하는 STOMP 요청
     * @param headerAccessor 인증 handle 확인과 응답 전송에 사용하는 STOMP 헤더
     */
    @MessageMapping({"problem.submit", "judge.submit"})
    public void handleProblemSubmit(ProblemSocketReq request, SimpMessageHeaderAccessor headerAccessor) {
        StompReplyTarget replyTarget = problemSupport.createStompReplyTarget(headerAccessor);
        ProblemSubmissionInput input = request.toSubmissionInput(
                replyTarget,
                progress -> sendToSession(replyTarget, ProblemSubmitProgressRes.from(progress))
        );

        ProblemExecuteRes response = ProblemExecuteRes.submitResult(submitProblemSql.execute(input));
        sendToSession(replyTarget, response);
    }

    /**
     * 진행 중인 문제 SQL 실행 취소 STOMP 요청을 처리한다.
     *
     * @param request 취소 요청 페이로드
     * @param headerAccessor 취소 대상 실행 세션을 식별하는 STOMP 헤더
     */
    @MessageMapping({"problem.execute.stop", "judge.execute.stop"})
    public void handleProblemExecuteStop(ProblemSocketReq request, SimpMessageHeaderAccessor headerAccessor) {
        String executionSessionId = problemSupport.createExecutionSessionId(problemSupport.resolveStompSessionId(headerAccessor));

        cancelProblemExecution.execute(executionSessionId);
    }

    /**
     * 문제 풀이 화면 이탈 STOMP 요청을 처리한다.
     *
     * <ol>
     *   <li>judge 실행 환경 정리
     *   <li>이탈 성공 응답 전송
     * </ol>
     *
     * @param request 이탈 대상 문제 ID를 포함한 STOMP 요청
     * @param headerAccessor 정리 대상 실행 세션 식별과 응답 전송에 사용하는 STOMP 헤더
     */
    @MessageMapping({"problem.leave", "judge.leave"})
    public void handleProblemLeave(ProblemSocketReq request, SimpMessageHeaderAccessor headerAccessor) {
        StompReplyTarget replyTarget = problemSupport.createStompReplyTarget(headerAccessor);

        closeProblemExecutionSession.execute(replyTarget.getExecutionSessionId());
        sendToSession(replyTarget, ProblemExecuteRes.leaveSuccess(request.problemId()));
    }

    /**
     * STOMP 연결 종료 시 judge 실행 세션을 정리한다.
     *
     * <ol>
     *   <li>STOMP 세션 ID 확인
     *   <li>judge 실행 환경 정리
     * </ol>
     *
     * @param event 종료된 STOMP 세션 이벤트
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        closeProblemExecutionSession.execute(problemSupport.createExecutionSessionId(sessionId));
    }

    private void sendToSession(StompReplyTarget replyTarget, Object payload) {
        // STOMP 응답 전송
        sessionStompSender.sendToSessionUnchecked(replyTarget.getHandle(), replyTarget.getReplySessionId(), payload);
    }

}
