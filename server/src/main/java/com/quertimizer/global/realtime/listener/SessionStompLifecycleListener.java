package com.quertimizer.global.realtime.listener;

import com.quertimizer.global.realtime.registry.SessionStompRegistry;
import com.quertimizer.global.realtime.sender.SessionStompSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionStompLifecycleListener {

    private static final String SESSION_REPLY_DESTINATION = "/user/queue/session";

    private final SessionStompRegistry sessionStompRegistry;
    private final SessionStompSender sessionStompSender;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String httpSessionId = resolveHttpSessionId(headerAccessor);
        String handle = resolveHandle(headerAccessor);
        String stompSessionId = headerAccessor.getSessionId();

        sessionStompRegistry.register(httpSessionId, handle, stompSessionId);
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (!SESSION_REPLY_DESTINATION.equals(headerAccessor.getDestination())) {
            return;
        }

        String handle = resolveHandle(headerAccessor);
        String stompSessionId = headerAccessor.getSessionId();
        if (handle == null || handle.isBlank() || stompSessionId == null || stompSessionId.isBlank()) {
            return;
        }

        try {
            sessionStompSender.sendToSession(handle, stompSessionId, Map.of("type", "connected", "handle", handle));
        } catch (Exception exception) {
            log.warn("STOMP 연결 완료 메시지 전송에 실패했습니다.", exception);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        sessionStompRegistry.unregister(event.getSessionId());
    }

    private String resolveHttpSessionId(StompHeaderAccessor headerAccessor) {
        // handshake session attribute 기준 HTTP 세션 ID 조회
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        Object sessionId = sessionAttributes != null ? sessionAttributes.get("sessionId") : null;
        return sessionId instanceof String value ? value : null;
    }

    private String resolveHandle(StompHeaderAccessor headerAccessor) {
        // handshake session attribute 기준 handle 조회
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        Object handle = sessionAttributes != null ? sessionAttributes.get("handle") : null;
        if (handle instanceof String value && !value.isBlank()) {
            return value;
        }

        // STOMP Principal 기준 handle 조회
        Principal user = headerAccessor.getUser();
        return user != null ? user.getName() : null;
    }
}
