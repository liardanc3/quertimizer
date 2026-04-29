package com.quertimizer.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("WebSocketConfig")
class WebSocketConfigIntegrationTest {

    @Autowired private List<HandlerMapping> handlerMappings;

    @Nested
    @DisplayName("GET /ws/session")
    class RegisterSessionWebSocket {

        @Test
        @DisplayName("성공 (핸들러 등록)")
        void successWhenSessionWebSocketPathRegistered() {
            // given

            // when
            boolean registered = handlerMappings.stream()
                    .filter(SimpleUrlHandlerMapping.class::isInstance)
                    .map(SimpleUrlHandlerMapping.class::cast)
                    .anyMatch(handlerMapping -> handlerMapping.getUrlMap().containsKey("/ws/session"));

            // then
            assertThat(registered).isTrue();
        }
    }
}
