package com.quertimizer.global.config;

import com.quertimizer.global.websocket.interceptor.SessionHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final SessionHandshakeInterceptor sessionHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // WebSocket application destination과 사용자별 응답 queue 구성
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.enableSimpleBroker("/queue");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 세션 WebSocket endpoint 등록
        registry.addEndpoint("/ws/session")
                .addInterceptors(sessionHandshakeInterceptor)
                .setHandshakeHandler(sessionPrincipalHandshakeHandler())
                .setAllowedOriginPatterns(AllowedOriginPatterns.ARRAY);
    }

    private DefaultHandshakeHandler sessionPrincipalHandshakeHandler() {
        // WebSocket session attribute의 handle을 WebSocket Principal로 연결
        return new DefaultHandshakeHandler() {

            @Override
            protected Principal determineUser(ServerHttpRequest request,
                                              WebSocketHandler wsHandler,
                                              Map<String, Object> attributes) {
                String handle = (String) attributes.get("handle");
                if (handle == null || handle.isBlank()) {
                    return super.determineUser(request, wsHandler, attributes);
                }

                return () -> handle;
            }
        };
    }
}
