package com.quertimizer.global.realtime.router;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SessionSocketMessageRouter {

    private final List<SessionSocketMessageHandler> handlers;

    public void route(WebSocketSession session, SessionSocketMessage message) throws Exception {
        // 메시지 타입을 처리할 inbound handler로 라우팅
        for (SessionSocketMessageHandler handler : handlers) {
            if (handler.supports(message.type())) {
                handler.handle(session, message);
                return;
            }
        }
    }

    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 연결 종료 후 각 inbound handler가 가진 정리 작업을 수행
        for (SessionSocketMessageHandler handler : handlers) {
            handler.afterConnectionClosed(session, status);
        }
    }
}
