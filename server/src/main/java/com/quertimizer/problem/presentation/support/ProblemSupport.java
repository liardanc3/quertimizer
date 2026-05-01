package com.quertimizer.problem.presentation.support;

import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.domain.model.AuthFailReason;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProblemSupport {

    private final AuthService authService;

    public String resolveCurrentHandle(Authentication authentication) {
        // 스프링 시큐리티 인증 정보 기준 현재 사용자 handle 확인
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.resolveCurrentHandle(authentication.getName());
    }

    public String resolveAuthenticatedEmail(Authentication authentication) {
        // 스프링 시큐리티 인증 정보 기준 현재 사용자 이메일 확인
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }

    public StompReplyTarget createStompReplyTarget(SimpMessageHeaderAccessor headerAccessor) {
        // STOMP 응답 대상과 문제 실행 세션 ID 구성
        String stompSessionId = resolveStompSessionId(headerAccessor);
        return new StompReplyTarget(
                resolveStompAuthenticatedHandle(headerAccessor),
                stompSessionId,
                createExecutionSessionId(stompSessionId)
        );
    }

    public String resolveStompSessionId(SimpMessageHeaderAccessor headerAccessor) {
        // STOMP 세션 ID 확인
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(AuthFailReason.LOGIN_INFORMATION_NOT_FOUND.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        return sessionId;
    }

    public String createExecutionSessionId(String transportSessionId) {
        // 웹 전송 세션 기준 문제 실행 세션 ID 생성
        if (transportSessionId == null || transportSessionId.isBlank()) {
            throw new BusinessException(AuthFailReason.LOGIN_INFORMATION_NOT_FOUND.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        return "problem-execution-session:" + transportSessionId;
    }

    private String resolveStompAuthenticatedHandle(SimpMessageHeaderAccessor headerAccessor) {
        // 핸드셰이크 세션 속성 기준 인증 handle 확인
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        String handle = sessionAttributes != null ? (String) sessionAttributes.get("handle") : null;
        if (handle == null || handle.isBlank()) {
            // STOMP principal 기준 인증 handle 확인
            Principal user = headerAccessor.getUser();
            handle = user != null ? user.getName() : null;
        }

        // 인증 handle 부재 시 로그인 정보 오류 반환
        if (handle == null || handle.isBlank()) {
            throw new BusinessException(AuthFailReason.LOGIN_INFORMATION_NOT_FOUND.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        return handle;
    }

    public static final class StompReplyTarget {

        private final String handle;
        private final String replySessionId;
        private final String executionSessionId;

        private StompReplyTarget(String handle, String replySessionId, String executionSessionId) {
            this.handle = handle;
            this.replySessionId = replySessionId;
            this.executionSessionId = executionSessionId;
        }

        public String getHandle() {
            return handle;
        }

        public String getReplySessionId() {
            return replySessionId;
        }

        public String getExecutionSessionId() {
            return executionSessionId;
        }
    }
}
