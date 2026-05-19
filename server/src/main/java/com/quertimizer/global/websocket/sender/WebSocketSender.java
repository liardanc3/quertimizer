package com.quertimizer.global.websocket.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.log.LogFormatter;
import com.quertimizer.global.log.LogMdcContext;
import com.quertimizer.global.websocket.WebSocketDestination;
import com.quertimizer.global.websocket.registry.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSender {

    private final ObjectMapper objectMapper;
    private final LogFormatter logFormatter;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String handle, Object payload) throws Exception {
        // 사용자 handle 기반 전송 가능 여부 확인
        if (handle == null || handle.isBlank()) {
            return;
        }

        // registry에 등록된 사용자 세션이 있으면 세션 단위로 payload 전송
        Set<String> sessionIds = webSocketSessionRegistry.findSessionIdsByHandle(handle);
        if (!sessionIds.isEmpty()) {
            for (String sessionId : sessionIds) {
                sendToSession(handle, sessionId, payload);
            }
            return;
        }

        // registry 조회 실패 시 Spring user destination 기준 payload 전송
        sendToUserDestination(handle, payload);
    }

    public void sendToSession(String handle, String sessionId, Object payload) throws Exception {
        // WebSocket 세션 단위 전송 가능 여부 확인
        if (handle == null || handle.isBlank() || sessionId == null || sessionId.isBlank()) {
            return;
        }

        // 특정 WebSocket 세션으로 사용자 queue 메시지 전송
        sendToSessionDestination(handle, sessionId, payload);
    }

    public void sendToSessionUnchecked(String handle, String sessionId, Object payload) {
        try {
            // WebSocket 세션 단위 응답 전송
            sendToSession(handle, sessionId, payload);
        } catch (Exception exception) {
            // 전송 실패를 메시지 예외 흐름으로 전달
            throw new IllegalStateException(exception);
        }
    }

    public void sendToSessionSilently(String handle, String sessionId, Object payload) {
        try {
            // WebSocket 세션 단위 응답 전송 실패 무시
            sendToSession(handle, sessionId, payload);
        } catch (Exception exception) {
            log.warn("WebSocket 세션 메시지 전송 실패", exception);
        }
    }

    public void closeHttpSessionWebSockets(String sessionId) {
        // HTTP 세션에 연결된 WebSocket 세션 종료 메시지 전송
        for (Map.Entry<String, Set<String>> entry : webSocketSessionRegistry.takeHttpSessionTargets(sessionId).entrySet()) {
            for (String webSocketSessionId : entry.getValue()) {
                try {
                    sendToSession(entry.getKey(), webSocketSessionId, Map.of("type", "session.closed"));
                } catch (Exception exception) {
                    log.warn("WebSocket 세션 종료 메시지 전송에 실패했습니다.", exception);
                }
            }
        }
    }

    private void sendToUserDestination(String handle, Object payload) throws Exception {
        // 사용자 queue로 payload 전송
        logWebSocketPayload(handle, payload);
        messagingTemplate.convertAndSendToUser(handle, WebSocketDestination.SESSION_QUEUE, payload);
    }

    private void sendToSessionDestination(String handle, String sessionId, Object payload) throws Exception {
        // 사용자 queue와 WebSocket 세션을 지정해 payload 전송
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);

        logWebSocketPayload(handle, payload);
        messagingTemplate.convertAndSendToUser(handle, WebSocketDestination.SESSION_QUEUE, payload, headers.getMessageHeaders());
    }

    private void logWebSocketPayload(String actor, Object payload) throws Exception {
        // WebSocket 응답 payload 로그 기록
        try (LogMdcContext.LogActorScope ignored = LogMdcContext.openActorScope(actor)) {
            String serializedPayload = objectMapper.writeValueAsString(payload);

            log.debug("WebSocket server-send");
            logLines(logFormatter.formatResponseBodyLines("", serializedPayload));
        }
    }

    private void logLines(List<String> logLines) {
        // payload 로그 라인을 순서대로 기록
        for (String logLine : logLines) {
            log.debug("{}", logLine);
        }
    }
}
