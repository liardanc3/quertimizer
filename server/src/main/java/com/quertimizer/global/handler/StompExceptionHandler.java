package com.quertimizer.global.handler;

import com.quertimizer.global.constant.GlobalFailReason;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.realtime.sender.SessionStompSender;
import com.quertimizer.problem.domain.model.ProblemLogMessage;
import com.quertimizer.problem.presentation.realtime.dto.ProblemExecuteRes;
import com.quertimizer.problem.presentation.realtime.dto.ProblemSocketReq;
import com.quertimizer.problem.presentation.support.ProblemSupport;
import com.quertimizer.problem.presentation.support.ProblemSupport.StompReplyTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.List;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class StompExceptionHandler {

    private final SessionStompSender sessionStompSender;
    private final ProblemSupport problemSupport;

    /**
     * STOMP 처리 예외를 세션 응답으로 변환한다.
     *
     * <ol>
     *   <li>STOMP 응답 대상 확인
     *   <li>요청 경로 기준 실패 응답 전송
     * </ol>
     *
     * @param exception STOMP 메시지 핸들러에서 발생한 예외
     * @param message 예외가 발생한 STOMP 원본 메시지
     */
    @MessageExceptionHandler(Exception.class)
    public void handleStompException(Exception exception, Message<?> message) {
        String destination = SimpMessageHeaderAccessor.wrap(message).getDestination();

        try {
            StompReplyTarget replyTarget = problemSupport.createStompReplyTarget(SimpMessageHeaderAccessor.wrap(message));
            sessionStompSender.sendToSession(
                    replyTarget.getHandle(), replyTarget.getReplySessionId(),
                    createFailureResponse(destination, message.getPayload(), exception)
            );
        } catch (Exception sendException) {
            log.warn(resolveSendFailureLogMessage(destination).getMessage(), sendException);
        }
    }

    private ProblemExecuteRes createFailureResponse(String destination, Object payload, Exception exception) {
        // API 예외 응답과 동일한 기준으로 실패 사유 결정
        StompFailure failure = resolveFailure(exception);
        String problemId = resolveProblemId(payload);

        // STOMP 제출 요청 경로 실패 응답 생성
        if (isSubmitDestination(destination)) {
            return ProblemExecuteRes.submitFailure(problemId, failure.reason, failure.reasons);
        }

        // STOMP 실행 요청 경로 실패 응답 생성
        if (isExecuteDestination(destination)) {
            return ProblemExecuteRes.executionFailure(problemId, failure.reason, failure.reasons);
        }

        // 기본 STOMP 실패 응답 생성
        return ProblemExecuteRes.error(failure.reason, failure.reasons);
    }

    private ProblemLogMessage resolveSendFailureLogMessage(String destination) {
        // 실행 페이지 요청 경로 로그 메시지 결정
        if (isExecutePageDestination(destination)) {
            return ProblemLogMessage.EXECUTE_PAGE_RESPONSE_SEND_FAILED;
        }

        // 제출 요청 경로 로그 메시지 결정
        if (isSubmitDestination(destination)) {
            return ProblemLogMessage.SUBMIT_FAILURE_RESPONSE_SEND_FAILED;
        }

        // 기본 실행 요청 경로 로그 메시지 결정
        return ProblemLogMessage.EXECUTE_FAILURE_RESPONSE_SEND_FAILED;
    }

    private boolean isExecutePageDestination(String destination) {
        // 실행 페이지 요청 경로 여부 확인
        return destination != null && (destination.endsWith("problem.execute.page") || destination.endsWith("judge.execute.page"));
    }

    private boolean isExecuteDestination(String destination) {
        // 실행 요청 경로 여부 확인
        return destination != null && (destination.endsWith("problem.execute")
                || destination.endsWith("problem.execute.page")
                || destination.endsWith("judge.execute")
                || destination.endsWith("judge.execute.page"));
    }

    private boolean isSubmitDestination(String destination) {
        // 제출 요청 경로 여부 확인
        return destination != null && (destination.endsWith("problem.submit") || destination.endsWith("judge.submit"));
    }

    private StompFailure resolveFailure(Exception exception) {
        // BusinessException은 API 예외 처리와 동일하게 보유 사유 사용
        BusinessException businessException = findBusinessException(exception);
        if (businessException != null) {
            String reason = businessException.getReason();
            return new StompFailure(reason, List.of(reason));
        }

        // 관리하지 않는 예외는 공용 500 메시지로 은닉
        String reason = GlobalFailReason.UNEXPECTED_ERROR.getMessage();
        return new StompFailure(reason, List.of(reason));
    }

    private BusinessException findBusinessException(Throwable throwable) {
        // 예외 체인 내 BusinessException 조회
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return businessException;
            }

            current = current.getCause();
        }

        return null;
    }

    private String resolveProblemId(Object payload) {
        // STOMP 요청 페이로드에서 문제 ID 추출
        return payload instanceof ProblemSocketReq request ? request.problemId() : null;
    }

    private static final class StompFailure {
        private final String reason;
        private final List<String> reasons;

        private StompFailure(String reason, List<String> reasons) {
            this.reason = reason;
            this.reasons = reasons;
        }
    }
}
