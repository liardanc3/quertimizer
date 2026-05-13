package com.quertimizer.global.websocket.listener;

import com.quertimizer.global.websocket.WebSocketDestination;
import com.quertimizer.global.websocket.registry.WebSocketSessionRegistry;
import com.quertimizer.global.websocket.sender.WebSocketSender;
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
public class WebSocketSessionLifecycleListener {

    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final WebSocketSender webSocketSender;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String httpSessionId = resolveHttpSessionId(headerAccessor);
        String handle = resolveHandle(headerAccessor);
        String webSocketSessionId = headerAccessor.getSessionId();

        webSocketSessionRegistry.register(httpSessionId, handle, webSocketSessionId);
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (!WebSocketDestination.USER_SESSION_QUEUE.equals(headerAccessor.getDestination())) {
            return;
        }

        String handle = resolveHandle(headerAccessor);
        String webSocketSessionId = headerAccessor.getSessionId();
        if (handle == null || handle.isBlank() || webSocketSessionId == null || webSocketSessionId.isBlank()) {
            return;
        }

        try {
            webSocketSender.sendToSession(handle, webSocketSessionId, Map.of("type", "connected", "handle", handle));
        } catch (Exception exception) {
            log.warn("WebSocket 연결 완료 메시지 전송에 실패했습니다.", exception);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        webSocketSessionRegistry.unregister(event.getSessionId());
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

        // WebSocket Principal 기준 handle 조회
        Principal user = headerAccessor.getUser();
        return user != null ? user.getName() : null;
    }
}
