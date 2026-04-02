package com.quertimizer.endpoint.websocket.interceptor;

import com.quertimizer.logging.LogFormatter;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionHandshakeInterceptorTest {

    @InjectMocks
    private SessionHandshakeInterceptor sessionHandshakeInterceptor;

    @Mock
    private LogFormatter logFormatter;

    @Nested
    @DisplayName("/ws/session")
    class SessionSocket {

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("handshake 허용 및 user/session 정보 저장")
            void allowHandshake() {
                // given
                MockHttpServletRequest servletRequest = new MockHttpServletRequest();
                HttpSession session = servletRequest.getSession(true);
                Authentication authentication = new UsernamePasswordAuthenticationToken("tester", null, List.of());
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

                ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
                ServerHttpResponse response = mock(ServerHttpResponse.class);
                WebSocketHandler webSocketHandler = mock(WebSocketHandler.class);
                Map<String, Object> attributes = new HashMap<>();

                // when
                boolean isAllowed = sessionHandshakeInterceptor.beforeHandshake(request, response, webSocketHandler, attributes);

                // then
                assertThat(isAllowed).isTrue();
                assertThat(attributes.get("userId")).isEqualTo("tester");
                assertThat(attributes.get("sessionId")).isEqualTo(session.getId());
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("401 Unauthorized 반환 (세션 없음)")
            void rejectWhenSessionMissing() {
                // given
                MockHttpServletRequest servletRequest = new MockHttpServletRequest();
                ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
                ServerHttpResponse response = mock(ServerHttpResponse.class);
                WebSocketHandler webSocketHandler = mock(WebSocketHandler.class);
                Map<String, Object> attributes = new HashMap<>();

                // when
                boolean isAllowed = sessionHandshakeInterceptor.beforeHandshake(request, response, webSocketHandler, attributes);

                // then
                assertThat(isAllowed).isFalse();
                verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
            }
        }
    }
}
