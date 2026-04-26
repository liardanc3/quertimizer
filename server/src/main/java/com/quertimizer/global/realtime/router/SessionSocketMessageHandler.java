package com.quertimizer.global.realtime.router;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

public interface SessionSocketMessageHandler {

    boolean supports(String type);

    void handle(WebSocketSession session, SessionSocketMessage message) throws Exception;

    default void afterConnectionClosed(WebSocketSession session, CloseStatus status) {}
}
