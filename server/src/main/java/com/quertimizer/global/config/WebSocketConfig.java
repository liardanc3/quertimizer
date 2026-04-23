package com.quertimizer.global.config;

import com.quertimizer.problem.presentation.realtime.handler.SessionWebSocketHandler;
import com.quertimizer.problem.presentation.realtime.interceptor.SessionHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SessionWebSocketHandler sessionWebSocketHandler;
    private final SessionHandshakeInterceptor sessionHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(sessionWebSocketHandler, "/ws/session")
                .addInterceptors(sessionHandshakeInterceptor)
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }
}
