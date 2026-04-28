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

    /**
     * message type을 처리할 수 있는 도메인 inbound handler로 메시지를 전달한다.
     *
     * @param session 메시지를 수신한 WebSocket 세션
     * @param message 라우팅할 WebSocket 메시지
     * @throws Exception handler 실행에 실패한 경우
     */
    public void route(WebSocketSession session, SessionSocketMessage message) throws Exception {
        for (SessionSocketMessageHandler handler : handlers) {
            if (handler.supports(message.type())) {
                handler.handle(session, message);
                return;
            }
        }
    }

    /**
     * WebSocket 연결 종료 후 각 도메인 inbound handler의 정리 작업을 실행한다.
     *
     * @param session 종료된 WebSocket 세션
     * @param status 연결 종료 상태
     */
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        for (SessionSocketMessageHandler handler : handlers) {
            handler.afterConnectionClosed(session, status);
        }
    }
}
