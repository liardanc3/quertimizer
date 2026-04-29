package com.quertimizer.judge.presentation.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.realtime.router.SessionSocketMessage;
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
@DisplayName("JudgeSocketMessageHandler")
class JudgeSocketMessageHandlerIntegrationTest {

    @Autowired private JudgeSocketMessageHandler judgeSocketMessageHandler;
    @Autowired private ObjectMapper objectMapper;

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("성공 (judge message type)")
        void successWhenEveryJudgeTypeSupported() {
            // given
            List<String> types = List.of(
                    "problem.execute",
                    "problem.execute.page",
                    "problem.execute.stop",
                    "problem.submit",
                    "problem.leave",
                    "judge.execute",
                    "judge.execute.page",
                    "judge.execute.stop",
                    "judge.submit",
                    "judge.leave"
            );

            // when
            List<Boolean> supported = types.stream()
                    .map(judgeSocketMessageHandler::supports)
                    .toList();

            // then
            assertThat(supported).containsOnly(true);
        }

        @Test
        @DisplayName("실패 (지원하지 않는 type)")
        void falseWhenTypeUnsupported() {
            // given
            String type = "unknown.type";

            // when
            boolean supported = judgeSocketMessageHandler.supports(type);

            // then
            assertThat(supported).isFalse();
        }
    }

    @Nested
    @DisplayName("problem.leave")
    class ProblemLeave {

        @Test
        @DisplayName("성공 (workspace 이탈)")
        void successWhenLeaveRequested() throws Exception {
            // given
            String handle = "beginner01";
            String type = "problem.leave";
            String problemId = "P00001-00001";
            String requestPayload = "{\"type\":\"problem.leave\",\"problemId\":\"P00001-00001\"}";
            TestWebSocketSession session = new TestWebSocketSession();
            session.getAttributes().put("handle", handle);
            SessionSocketMessage message = new SessionSocketMessage(
                    type,
                    objectMapper.readTree(requestPayload)
            );

            // when
            judgeSocketMessageHandler.handle(session, message);

            // then
            assertThat(session.sentPayloads()).anySatisfy(sentPayload -> {
                assertThat(sentPayload).contains("\"type\":\"problem.leave.result\"");
                assertThat(sentPayload).contains("\"problemId\":\"" + problemId + "\"");
            });
        }

        @Test
        @DisplayName("실패 (빈 type)")
        void ignoreWhenTypeBlank() throws Exception {
            // given
            String handle = "beginner01";
            String type = "problem.leave";
            String requestPayload = "{\"type\":\"\",\"problemId\":\"P00001-00001\"}";
            TestWebSocketSession session = new TestWebSocketSession();
            session.getAttributes().put("handle", handle);
            SessionSocketMessage message = new SessionSocketMessage(
                    type,
                    objectMapper.readTree(requestPayload)
            );

            // when
            judgeSocketMessageHandler.handle(session, message);

            // then
            assertThat(session.sentPayloads()).isEmpty();
        }
    }

    @Nested
    @DisplayName("problem.execute.stop")
    class ProblemExecuteStop {

        @Test
        @DisplayName("성공 (실행 취소)")
        void successWhenStopRequested() throws Exception {
            // given
            String type = "problem.execute.stop";
            String requestPayload = "{\"type\":\"problem.execute.stop\"}";
            TestWebSocketSession session = new TestWebSocketSession();
            SessionSocketMessage message = new SessionSocketMessage(
                    type,
                    objectMapper.readTree(requestPayload)
            );

            // when
            judgeSocketMessageHandler.handle(session, message);

            // then
            assertThat(session.sentPayloads()).isEmpty();
        }
    }

    @Nested
    @DisplayName("afterConnectionClosed")
    class AfterConnectionClosed {

        @Test
        @DisplayName("성공 (연결 종료 정리)")
        void successWhenConnectionClosed() {
            // given
            TestWebSocketSession session = new TestWebSocketSession();
            CloseStatus closeStatus = CloseStatus.NORMAL;

            // when
            judgeSocketMessageHandler.afterConnectionClosed(session, closeStatus);

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
