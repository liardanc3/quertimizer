package com.quertimizer.global.websocket.interceptor;

import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.GlobalFailReason;
import com.quertimizer.global.ratelimit.InMemoryGlobalRateLimiter;
import com.quertimizer.global.websocket.sender.WebSocketSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.WEBSOCKET_ADMIN_LONG_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.WEBSOCKET_ADMIN_SHORT_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.WEBSOCKET_ANONYMOUS_LONG_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.WEBSOCKET_ANONYMOUS_SHORT_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.WEBSOCKET_AUTHENTICATED_LONG_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.WEBSOCKET_AUTHENTICATED_SHORT_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.WEBSOCKET_LONG_WINDOW;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.WEBSOCKET_SHORT_WINDOW;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketRateLimitInterceptor implements ChannelInterceptor {

    private final InMemoryGlobalRateLimiter rateLimiter;
    private final ObjectProvider<WebSocketSender> webSocketSenderProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 애플리케이션 inbound 메시지가 아니면 제한 생략
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
        if (!shouldLimit(headerAccessor)) {
            return message;
        }

        // WebSocket 사용자 또는 IP 기준 rate limit 적용
        RateLimitTarget target = resolveTarget(headerAccessor);
        try {
            rateLimiter.validateAndRecord(
                    "ws:short:" + target.key, WEBSOCKET_SHORT_WINDOW, target.shortLimit,
                    "ws:long:" + target.key, WEBSOCKET_LONG_WINDOW, target.longLimit
            );
            return message;
        } catch (DomainRuleViolationException exception) {
            log.warn("[RateLimit] WebSocket 요청 제한 destination={} key={}", headerAccessor.getDestination(), target.key);
            sendRateLimitMessage(headerAccessor);
            return null;
        }
    }

    private boolean shouldLimit(StompHeaderAccessor headerAccessor) {
        // 클라이언트 SEND 중 /app destination만 제한
        String destination = headerAccessor.getDestination();
        return headerAccessor.getCommand() == StompCommand.SEND
                && destination != null
                && destination.startsWith("/app/");
    }

    private RateLimitTarget resolveTarget(StompHeaderAccessor headerAccessor) {
        // 인증 handle이 있으면 사용자 기준으로 제한
        String handle = resolveHandle(headerAccessor);
        boolean admin = isAdmin(headerAccessor);
        if (handle != null && !handle.isBlank()) {
            return new RateLimitTarget(
                    "user:" + normalize(handle),
                    admin ? WEBSOCKET_ADMIN_SHORT_LIMIT : WEBSOCKET_AUTHENTICATED_SHORT_LIMIT,
                    admin ? WEBSOCKET_ADMIN_LONG_LIMIT : WEBSOCKET_AUTHENTICATED_LONG_LIMIT
            );
        }

        // 인증 handle이 없으면 IP 기준으로 제한
        return new RateLimitTarget(
                "ip:" + normalize(resolveSessionAttribute(headerAccessor, "clientIp")),
                WEBSOCKET_ANONYMOUS_SHORT_LIMIT,
                WEBSOCKET_ANONYMOUS_LONG_LIMIT
        );
    }

    private String resolveHandle(StompHeaderAccessor headerAccessor) {
        // handshake session attribute 기준 handle 조회
        String handle = resolveSessionAttribute(headerAccessor, "handle");
        if (handle != null && !handle.isBlank()) {
            return handle;
        }

        // WebSocket Principal 기준 handle 조회
        Principal user = headerAccessor.getUser();
        return user != null ? user.getName() : null;
    }

    private boolean isAdmin(StompHeaderAccessor headerAccessor) {
        // handshake session attribute 기준 관리자 여부 확인
        Object admin = resolveRawSessionAttribute(headerAccessor, "admin");
        return admin instanceof Boolean value && value;
    }

    private void sendRateLimitMessage(StompHeaderAccessor headerAccessor) {
        // 요청한 WebSocket 세션으로 공통 요청 제한 메시지 전송
        String handle = resolveHandle(headerAccessor);
        String sessionId = headerAccessor.getSessionId();
        if (handle == null || handle.isBlank() || sessionId == null || sessionId.isBlank()) {
            return;
        }

        WebSocketSender webSocketSender = webSocketSenderProvider.getIfAvailable();
        if (webSocketSender == null) {
            return;
        }

        webSocketSender.sendToSessionSilently(handle, sessionId, Map.of(
                "type", "error",
                "success", false,
                "message", GlobalFailReason.REQUEST_RATE_LIMITED.getMessage(),
                "reasons", List.of(GlobalFailReason.REQUEST_RATE_LIMIT_DETAIL.getMessage())
        ));
    }

    private String resolveSessionAttribute(StompHeaderAccessor headerAccessor, String key) {
        // 세션 속성 문자열 값 조회
        Object value = resolveRawSessionAttribute(headerAccessor, key);
        return value instanceof String stringValue ? stringValue : null;
    }

    private Object resolveRawSessionAttribute(StompHeaderAccessor headerAccessor, String key) {
        // WebSocket session attribute 원본 값 조회
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        return sessionAttributes != null ? sessionAttributes.get(key) : null;
    }

    private String normalize(String value) {
        // 제한 key에 사용할 수 있도록 빈 식별값 정리
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private static final class RateLimitTarget {

        private final String key;
        private final int shortLimit;
        private final int longLimit;

        private RateLimitTarget(String key, int shortLimit, int longLimit) {
            this.key = key;
            this.shortLimit = shortLimit;
            this.longLimit = longLimit;
        }
    }
}
