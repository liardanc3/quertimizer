package com.quertimizer.global.websocket.interceptor;

import com.quertimizer.global.exception.GlobalFailReason;
import com.quertimizer.global.ratelimit.InMemoryGlobalRateLimiter;
import com.quertimizer.global.websocket.sender.WebSocketSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.WEBSOCKET_AUTHENTICATED_SHORT_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("WebSocketRateLimitInterceptor")
class WebSocketRateLimitInterceptorTest {

    private final WebSocketSender webSocketSender = mock(WebSocketSender.class);
    private final WebSocketRateLimitInterceptor interceptor =
            new WebSocketRateLimitInterceptor(new InMemoryGlobalRateLimiter(), new TestWebSocketSenderProvider(webSocketSender));

    @Nested
    @DisplayName("preSend")
    class PreSend {

        @Test
        @DisplayName("성공 (/app SEND 제한 미만 통과)")
        void successWhenAppSendBelowLimit() {
            // given
            Message<byte[]> message = message(StompCommand.SEND, "/app/problem.execute", "solver", "127.0.0.1", false);

            // when
            Message<?> result = interceptor.preSend(message, null);

            // then
            assertThat(result).isSameAs(message);
        }

        @Test
        @DisplayName("실패 (/app SEND 제한 초과)")
        void failWhenAppSendLimitExceeded() {
            // given
            for (int count = 0; count < WEBSOCKET_AUTHENTICATED_SHORT_LIMIT; count++) {
                interceptor.preSend(message(StompCommand.SEND, "/app/problem.execute", "solver", "127.0.0.1", false), null);
            }
            Message<byte[]> blockedMessage = message(StompCommand.SEND, "/app/problem.submit", "solver", "127.0.0.1", false);

            // when
            Message<?> result = interceptor.preSend(blockedMessage, null);

            // then
            assertThat(result).isNull();
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(webSocketSender).sendToSessionSilently(eq("solver"), eq("ws-1"), payloadCaptor.capture());
            assertThat(payloadCaptor.getValue())
                    .isInstanceOfSatisfying(Map.class, payload -> {
                        assertThat(payload).containsEntry("type", "error");
                        assertThat(payload).containsEntry("message", GlobalFailReason.REQUEST_RATE_LIMITED.getMessage());
                    });
        }

        @Test
        @DisplayName("성공 (SUBSCRIBE와 CONNECT 제한 제외)")
        void successWhenCommandExcluded() {
            // given
            Message<byte[]> subscribeMessage = message(StompCommand.SUBSCRIBE, "/app/problem.execute", "solver", "127.0.0.1", false);
            Message<byte[]> connectMessage = message(StompCommand.CONNECT, null, "solver", "127.0.0.1", false);

            // when
            Message<?> subscribeResult = interceptor.preSend(subscribeMessage, null);
            Message<?> connectResult = interceptor.preSend(connectMessage, null);

            // then
            assertThat(subscribeResult).isSameAs(subscribeMessage);
            assertThat(connectResult).isSameAs(connectMessage);
            verify(webSocketSender, never()).sendToSessionSilently(eq("solver"), eq("ws-1"), org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("성공 (/app 이외 destination 제외)")
        void successWhenDestinationExcluded() {
            // given
            Message<byte[]> message = message(StompCommand.SEND, "/topic/problem.execute", "solver", "127.0.0.1", false);

            // when
            Message<?> result = interceptor.preSend(message, null);

            // then
            assertThat(result).isSameAs(message);
            verify(webSocketSender, never()).sendToSessionSilently(eq("solver"), eq("ws-1"), org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("성공 (handle별 key 분리)")
        void successWhenHandleKeysAreDifferent() {
            // given
            for (int count = 0; count < WEBSOCKET_AUTHENTICATED_SHORT_LIMIT; count++) {
                interceptor.preSend(message(StompCommand.SEND, "/app/problem.execute", "solver-a", "127.0.0.1", false), null);
            }
            Message<byte[]> message = message(StompCommand.SEND, "/app/problem.execute", "solver-b", "127.0.0.1", false);

            // when
            Message<?> result = interceptor.preSend(message, null);

            // then
            assertThat(result).isSameAs(message);
        }

        @Test
        @DisplayName("성공 (clientIp fallback)")
        void successWhenClientIpFallbackUsed() {
            // given
            for (int count = 0; count < WEBSOCKET_AUTHENTICATED_SHORT_LIMIT; count++) {
                interceptor.preSend(message(StompCommand.SEND, "/app/problem.execute", null, "127.0.0.1", false), null);
            }
            Message<byte[]> message = message(StompCommand.SEND, "/app/problem.execute", null, "127.0.0.2", false);

            // when
            Message<?> result = interceptor.preSend(message, null);

            // then
            assertThat(result).isSameAs(message);
        }

        @Test
        @DisplayName("성공 (관리자 제한 적용)")
        void successWhenAdminLimitApplied() {
            // given
            for (int count = 0; count < WEBSOCKET_AUTHENTICATED_SHORT_LIMIT + 1; count++) {
                interceptor.preSend(message(StompCommand.SEND, "/app/monitoring.resources", "admin", "127.0.0.1", true), null);
            }
            Message<byte[]> message = message(StompCommand.SEND, "/app/monitoring.database-status", "admin", "127.0.0.1", true);

            // when
            Message<?> result = interceptor.preSend(message, null);

            // then
            assertThat(result).isSameAs(message);
        }
    }

    private static Message<byte[]> message(StompCommand command, String destination,
                                           String handle, String clientIp, boolean admin) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(command);
        headerAccessor.setSessionId("ws-1");
        if (destination != null) {
            headerAccessor.setDestination(destination);
        }
        Map<String, Object> sessionAttributes = new HashMap<>();
        if (handle != null) {
            sessionAttributes.put("handle", handle);
        }
        sessionAttributes.put("clientIp", clientIp);
        sessionAttributes.put("admin", admin);
        headerAccessor.setSessionAttributes(sessionAttributes);
        headerAccessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
    }

    private record TestWebSocketSenderProvider(WebSocketSender webSocketSender) implements ObjectProvider<WebSocketSender> {

        @Override
        public WebSocketSender getObject(Object... args) {
            return webSocketSender;
        }

        @Override
        public WebSocketSender getIfAvailable() {
            return webSocketSender;
        }

        @Override
        public WebSocketSender getIfUnique() {
            return webSocketSender;
        }

        @Override
        public WebSocketSender getObject() {
            return webSocketSender;
        }
    }
}
