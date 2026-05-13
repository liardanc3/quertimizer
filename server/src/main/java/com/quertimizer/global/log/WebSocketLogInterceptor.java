package com.quertimizer.global.log;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

@Component
public class WebSocketLogInterceptor implements ExecutorChannelInterceptor {

    private final ThreadLocal<LogMdcContext.LogActorScope> actorScope = new ThreadLocal<>();

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        // WebSocket handler 실행 스레드에 사용자 로그 주체 반영
        closeActorScope();
        actorScope.set(LogMdcContext.openActorScope(resolveActor(message)));
        return message;
    }

    @Override
    public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception exception) {
        // WebSocket handler 실행 후 사용자 로그 주체 정리
        closeActorScope();
    }

    private String resolveActor(Message<?> message) {
        // handshake session attribute 기준 사용자 식별자 조회
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        Object handle = sessionAttributes != null ? sessionAttributes.get("handle") : null;
        if (handle instanceof String value && !value.isBlank()) {
            return value;
        }

        // WebSocket Principal 기준 사용자 식별자 조회
        Principal user = headerAccessor.getUser();
        return user != null ? user.getName() : "";
    }

    private void closeActorScope() {
        // 이전 로그 주체 scope 복원
        LogMdcContext.LogActorScope previousScope = actorScope.get();
        if (previousScope == null) {
            return;
        }

        previousScope.close();
        actorScope.remove();
    }
}
