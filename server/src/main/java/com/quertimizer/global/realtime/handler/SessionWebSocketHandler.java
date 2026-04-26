package com.quertimizer.global.realtime.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.log.LogFormatter;
import com.quertimizer.global.realtime.registry.SessionSocketRegistry;
import com.quertimizer.global.realtime.router.SessionSocketMessage;
import com.quertimizer.global.realtime.router.SessionSocketMessageRouter;
import com.quertimizer.global.realtime.sender.SessionSocketSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final LogFormatter logFormatter;
    private final SessionSocketRegistry sessionSocketRegistry;
    private final SessionSocketSender sessionSocketSender;
    private final SessionSocketMessageRouter sessionSocketMessageRouter;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 연결 주체와 세션 연결 대상을 공용 registry에 등록
        String actor = resolveActor(session);
        String handle = (String) session.getAttributes().get("handle");
        String sessionId = (String) session.getAttributes().get("sessionId");

        sessionSocketRegistry.registerSessionSocket(sessionId, session);
        sessionSocketRegistry.registerUserSocket(handle, session);

        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket connection open", null));
        sessionSocketSender.sendObjectMessage(session, Map.of(
                "type", "connected",
                "handle", handle != null ? handle : ""
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 공용 WebSocket handler는 payload를 로깅하고 message routing만 담당
        String actor = resolveActor(session);
        String prefix = logFormatter.prefix(actor);

        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket client-send", null));
        logLines(logFormatter.formatRequestBodyLines(prefix, message.getPayload()));

        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText("");
            if (type.isBlank()) {
                return;
            }

            sessionSocketMessageRouter.route(session, new SessionSocketMessage(type, payload));
        } catch (Exception exception) {
            sessionSocketSender.sendObjectMessage(session, Map.of(
                    "type", "error",
                    "success", false,
                    "message", resolveErrorMessage(exception)
            ));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 연결 종료 후 registry와 각 도메인 inbound handler 정리 로직을 실행
        String actor = resolveActor(session);
        String sessionId = (String) session.getAttributes().get("sessionId");
        String handle = (String) session.getAttributes().get("handle");

        sessionSocketRegistry.unregisterSessionSocket(sessionId, session);
        sessionSocketRegistry.unregisterUserSocket(handle, session);
        sessionSocketMessageRouter.afterConnectionClosed(session, status);

        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket connection close", status.toString()));
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

    private String resolveErrorMessage(Exception exception) {
        // 공용 error payload 메시지를 결정
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }

        return "세션 메시지 처리에 실패했다.";
    }

    private void logLines(List<String> logLines) {
        // payload 로그 라인을 순서대로 기록
        for (String logLine : logLines) {
            log.info("{}", logLine);
        }
    }
}
