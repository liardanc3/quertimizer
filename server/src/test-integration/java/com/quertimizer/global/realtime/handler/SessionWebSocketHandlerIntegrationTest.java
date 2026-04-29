package com.quertimizer.global.realtime.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SessionWebSocketHandler")
class SessionWebSocketHandlerIntegrationTest {

    @Autowired private SessionWebSocketHandler sessionWebSocketHandler;

    @Nested
    @DisplayName("WebSocket connect /ws/session")
    class AfterConnectionEstablished {

        @Test
        @DisplayName("성공 (연결 메시지 전송)")
        void successWhenConnectionEstablished() throws Exception {
            // given
            String handle = "beginner01";
            String sessionId = "http-session-1";
            TestWebSocketSession session = new TestWebSocketSession();
            session.getAttributes().put("handle", handle);
            session.getAttributes().put("sessionId", sessionId);

            // when
            sessionWebSocketHandler.afterConnectionEstablished(session);

            // then
            assertThat(session.sentPayloads()).anySatisfy(payload -> {
                assertThat(payload).contains("\"type\":\"connected\"");
                assertThat(payload).contains("\"handle\":\"" + handle + "\"");
            });
        }
    }

    @Nested
    @DisplayName("WebSocket message /ws/session")
    class HandleTextMessage {

        @Test
        @DisplayName("성공 (빈 type 무시)")
        void successWhenTypeBlank() throws Exception {
            // given
            String payload = "{\"type\":\"\"}";
            TestWebSocketSession session = new TestWebSocketSession();
            TextMessage message = new TextMessage(payload);

            // when
            sessionWebSocketHandler.handleMessage(session, message);

            // then
            assertThat(session.sentPayloads()).isEmpty();
        }

        @Test
        @DisplayName("Throw (JSON 변환 실패)")
        void errorMessageWhenJsonInvalid() throws Exception {
            // given
            String payload = "{";
            TestWebSocketSession session = new TestWebSocketSession();
            TextMessage message = new TextMessage(payload);

            // when
            sessionWebSocketHandler.handleMessage(session, message);

            // then
            assertThat(session.sentPayloads()).anySatisfy(sentPayload -> {
                assertThat(sentPayload).contains("\"type\":\"error\"");
                assertThat(sentPayload).contains("\"success\":false");
            });
        }
    }

    @Nested
    @DisplayName("WebSocket close /ws/session")
    class AfterConnectionClosed {

        @Test
        @DisplayName("성공 (연결 종료)")
        void successWhenConnectionClosed() {
            // given
            String handle = "beginner01";
            String sessionId = "http-session-1";
            CloseStatus closeStatus = CloseStatus.NORMAL;
            TestWebSocketSession session = new TestWebSocketSession();
            session.getAttributes().put("handle", handle);
            session.getAttributes().put("sessionId", sessionId);

            // when
            sessionWebSocketHandler.afterConnectionClosed(session, closeStatus);

            // then
            assertThat(session.isOpen()).isTrue();
        }
    }

    static class TestWebSocketSession implements WebSocketSession {

        private final String id = "test-session";
        private final Map<String, Object> attributes = new HashMap<>();
        private final List<String> sentPayloads = new ArrayList<>();
        private boolean open = true;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public URI getUri() {
            return URI.create("ws://localhost/ws/session");
        }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return new HttpHeaders();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return new InetSocketAddress("127.0.0.1", 8080);
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return new InetSocketAddress("127.0.0.1", 10000);
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 8192;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 8192;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return List.of();
        }

        @Override
        public void sendMessage(WebSocketMessage<?> message) {
            if (message instanceof TextMessage textMessage) {
                sentPayloads.add(textMessage.getPayload());
            }
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() throws IOException {
            open = false;
        }

        @Override
        public void close(CloseStatus status) throws IOException {
            open = false;
        }

        List<String> sentPayloads() {
            return sentPayloads;
        }
    }
}
