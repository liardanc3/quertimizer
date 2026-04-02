package com.quertimizer.endpoint.websocket.handler;

import com.quertimizer.logging.LogFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionWebSocketHandler extends TextWebSocketHandler {

    private final LogFormatter logFormatter;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionSockets = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String actor = resolveActor(session);
        String userId = (String) session.getAttributes().get("userId");
        String sessionId = (String) session.getAttributes().get("sessionId");

        // 같은 HttpSession에서 열린 WebSocket 연결 추적
        if (sessionId != null && !sessionId.isBlank()) {
            sessionSockets.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet()).add(session);
        }

        // 연결 완료 로그 기록 후 초기 연결 메시지 전송
        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket connection open", null));
        sendTextMessage(session, "{\"type\":\"connected\",\"userId\":\"" + userId + "\"}");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String actor = resolveActor(session);
        String prefix = logFormatter.prefix(actor);

        // 클라이언트에서 보낸 payload 로그 기록
        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket client-send", null));
        logLines(logFormatter.formatRequestBodyLines(prefix, message.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String actor = resolveActor(session);
        String sessionId = (String) session.getAttributes().get("sessionId");

        if (sessionId != null && !sessionId.isBlank()) {
            removeSessionSocket(sessionId, session);
        }

        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket connection close", status.toString()));
    }

    public void closeSessionSockets(String sessionId) {

        // 로그아웃한 HttpSession에 연결된 모든 WebSocket 종료
        Set<WebSocketSession> webSocketSessions = sessionSockets.remove(sessionId);
        if (webSocketSessions == null) {
            return;
        }

        for (WebSocketSession webSocketSession : webSocketSessions) {

            if (!webSocketSession.isOpen()) {
                continue;
            }

            try {
                webSocketSession.close(CloseStatus.NORMAL);
            } catch (IOException ignored) {
            }
        }
    }

    private void sendTextMessage(WebSocketSession session, String payload) throws Exception {
        String actor = resolveActor(session);
        String prefix = logFormatter.prefix(actor);

        // 서버에서 보내는 payload도 동일한 형식으로 로그 기록
        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket server-send", null));
        logLines(logFormatter.formatResponseBodyLines(prefix, payload));
        session.sendMessage(new TextMessage(payload));
    }

    private String resolveActor(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");

        if (userId != null && !userId.isBlank()) {
            return userId;
        }

        if (session.getRemoteAddress() != null && session.getRemoteAddress().getAddress() != null) {
            return session.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    private void removeSessionSocket(String sessionId, WebSocketSession session) {
        sessionSockets.computeIfPresent(sessionId, (key, sessions) -> {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                return null;
            }

            return sessions;
        });
    }

    private void logLines(java.util.List<String> logLines) {
        for (String logLine : logLines) {
            log.info("{}", logLine);
        }
    }

}
