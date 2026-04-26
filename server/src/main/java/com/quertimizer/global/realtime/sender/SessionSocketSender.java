package com.quertimizer.global.realtime.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.log.LogFormatter;
import com.quertimizer.global.realtime.registry.SessionSocketRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionSocketSender {

    private final ObjectMapper objectMapper;
    private final LogFormatter logFormatter;
    private final SessionSocketRegistry sessionSocketRegistry;

    public void sendObjectMessage(WebSocketSession session, Object payload) throws Exception {
        // 객체 payload를 JSON 문자열로 직렬화 후 전송
        sendTextMessage(session, objectMapper.writeValueAsString(payload));
    }

    public void sendTextMessage(WebSocketSession session, String payload) throws Exception {
        // WebSocket 텍스트 메시지를 전송
        String actor = resolveActor(session);
        String prefix = logFormatter.prefix(actor);

        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket server-send", null));
        logLines(logFormatter.formatResponseBodyLines(prefix, payload));

        synchronized (session) {
            if (!session.isOpen()) {
                return;
            }

            session.sendMessage(new TextMessage(payload));
        }
    }

    public void sendToUser(String handle, Object payload) throws Exception {
        // 사용자 Handle 기준으로 payload를 브로드캐스트
        Set<WebSocketSession> userSockets = sessionSocketRegistry.findUserSockets(handle);
        if (userSockets.isEmpty()) {
            return;
        }

        for (WebSocketSession userSocket : userSockets) {
            if (!userSocket.isOpen()) {
                sessionSocketRegistry.unregisterUserSocket(handle, userSocket);
                continue;
            }

            sendObjectMessage(userSocket, payload);
        }
    }

    public void closeSessionSockets(String sessionId) {
        // 같은 HttpSession에 연결된 모든 WebSocket 연결을 종료
        for (WebSocketSession session : sessionSocketRegistry.takeSessionSockets(sessionId)) {
            if (!session.isOpen()) {
                continue;
            }

            try {
                session.close(CloseStatus.NORMAL);
            } catch (IOException ignored) {
            }
        }
    }

    private String resolveActor(WebSocketSession session) {
        // WebSocket 로그 주체를 결정
        String handle = (String) session.getAttributes().get("handle");
        if (handle != null && !handle.isBlank()) {
            return handle;
        }

        if (session.getRemoteAddress() != null && session.getRemoteAddress().getAddress() != null) {
            return session.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    private void logLines(List<String> logLines) {
        // payload 로그 라인을 순서대로 기록
        for (String logLine : logLines) {
            log.info("{}", logLine);
        }
    }
}
