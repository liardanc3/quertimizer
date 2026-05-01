package com.quertimizer.global.realtime.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.log.LogFormatter;
import com.quertimizer.global.realtime.registry.SessionStompRegistry;
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
public class SessionStompSender {

    private static final String SESSION_REPLY_DESTINATION = "/queue/session";

    private final ObjectMapper objectMapper;
    private final LogFormatter logFormatter;
    private final SessionStompRegistry sessionStompRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String handle, Object payload) throws Exception {
        // 사용자 handle 기반 전송 가능 여부 확인
        if (handle == null || handle.isBlank()) {
            return;
        }

        // 사용자 전체 STOMP 세션에 payload 전송
        logStompPayload(handle, payload);
        messagingTemplate.convertAndSendToUser(handle, SESSION_REPLY_DESTINATION, payload);
    }

    public void sendToSession(String handle, String sessionId, Object payload) throws Exception {
        // STOMP 세션 단위 전송 가능 여부 확인
        if (handle == null || handle.isBlank() || sessionId == null || sessionId.isBlank()) {
            return;
        }

        // 특정 STOMP 세션으로 사용자 queue 메시지 전송
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);

        logStompPayload(handle, payload);
        messagingTemplate.convertAndSendToUser(handle, SESSION_REPLY_DESTINATION, payload, headers.getMessageHeaders());
    }

    public void sendToSessionUnchecked(String handle, String sessionId, Object payload) {
        try {
            // STOMP 세션 단위 응답 전송
            sendToSession(handle, sessionId, payload);
        } catch (Exception exception) {
            // 전송 실패를 메시지 예외 흐름으로 전달
            throw new IllegalStateException(exception);
        }
    }

    public void closeHttpSessionStompSessions(String sessionId) {
        // HTTP 세션에 연결된 STOMP 세션 종료 메시지 전송
        for (Map.Entry<String, Set<String>> entry : sessionStompRegistry.takeHttpSessionTargets(sessionId).entrySet()) {
            for (String stompSessionId : entry.getValue()) {
                try {
                    sendToSession(entry.getKey(), stompSessionId, Map.of("type", "session.closed"));
                } catch (Exception exception) {
                    log.warn("STOMP 세션 종료 메시지 전송에 실패했다.", exception);
                }
            }
        }
    }

    private void logStompPayload(String actor, Object payload) throws Exception {
        // STOMP 응답 payload 로그 기록
        String prefix = logFormatter.prefix(actor);
        String serializedPayload = objectMapper.writeValueAsString(payload);

        log.info("{}", logFormatter.formatWebSocketLine(actor, "STOMP server-send", null));
        logLines(logFormatter.formatResponseBodyLines(prefix, serializedPayload));
    }

    private void logLines(List<String> logLines) {
        // payload 로그 라인을 순서대로 기록
        for (String logLine : logLines) {
            log.info("{}", logLine);
        }
    }
}
