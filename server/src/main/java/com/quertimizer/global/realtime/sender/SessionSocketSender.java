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

    /**
     * 객체 payload를 JSON 문자열로 직렬화해 WebSocket으로 전송한다.
     *
     * @param session 메시지를 보낼 WebSocket 세션
     * @param payload 직렬화할 응답 payload
     * @throws Exception JSON 직렬화 또는 WebSocket 전송에 실패한 경우
     */
    public void sendObjectMessage(WebSocketSession session, Object payload) throws Exception {
        sendTextMessage(session, objectMapper.writeValueAsString(payload));
    }

    /**
     * WebSocket 텍스트 메시지를 로그에 남긴 뒤 전송한다.
     *
     * @param session 메시지를 보낼 WebSocket 세션
     * @param payload 전송할 텍스트 payload
     * @throws Exception WebSocket 전송에 실패한 경우
     */
    public void sendTextMessage(WebSocketSession session, String payload) throws Exception {
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

    /**
     * 사용자 handle에 연결된 모든 WebSocket 세션으로 payload를 전송한다.
     *
     * @param handle 전송 대상 사용자 handle
     * @param payload 전송할 응답 payload
     * @throws Exception JSON 직렬화 또는 WebSocket 전송에 실패한 경우
     */
    public void sendToUser(String handle, Object payload) throws Exception {
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

    /**
     * 같은 HttpSession에 연결된 모든 WebSocket 세션을 종료한다.
     *
     * @param sessionId 종료할 HTTP 세션 ID
     */
    public void closeSessionSockets(String sessionId) {
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
