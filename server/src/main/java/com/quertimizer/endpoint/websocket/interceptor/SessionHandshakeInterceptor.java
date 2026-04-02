package com.quertimizer.endpoint.websocket.interceptor;

import com.quertimizer.logging.LogFormatter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionHandshakeInterceptor implements HandshakeInterceptor {

    private final LogFormatter logFormatter;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String actor = resolveActor(request);
        String prefix = logFormatter.prefix(actor);

        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket handshake request", null));
        logLines(logFormatter.formatQueryStringLines(prefix, request.getURI().getQuery()));

        // Servlet 요청이 아니면 인증 세션을 확인할 수 없어 연결 거부
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
        HttpSession session = httpServletRequest.getSession(false);
        Authentication authentication = resolveAuthentication(httpServletRequest, session);

        if (!isAuthenticatedUser(authentication)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (session == null) {
            session = httpServletRequest.getSession(true);
        }

        // WebSocket 세션에서 사용할 userId와 HttpSession id 전달
        attributes.put("userId", authentication.getName());
        attributes.put("sessionId", session.getId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception == null) {
            log.info("{}", logFormatter.formatWebSocketLine(resolveActor(request), "WebSocket handshake success", null));
        }
    }

    private String resolveActor(ServerHttpRequest request) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return "unknown";
        }

        HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
        Authentication authentication = resolveAuthentication(httpServletRequest, httpServletRequest.getSession(false));

        if (isAuthenticatedUser(authentication)) {
            return authentication.getName();
        }

        return httpServletRequest.getRemoteAddr();
    }

    private Authentication resolveAuthentication(HttpServletRequest httpServletRequest, HttpSession session) {

        // 세션에 저장된 SecurityContext에서 인증정보 우선 조회
        if (session != null) {
            Object context = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (context instanceof SecurityContext securityContext) {
                Authentication authentication = securityContext.getAuthentication();
                if (isAuthenticatedUser(authentication)) {
                    return authentication;
                }
            }
        }

        if (httpServletRequest.getUserPrincipal() instanceof Authentication authentication && isAuthenticatedUser(authentication)) {
            return authentication;
        }

        return null;
    }

    private boolean isAuthenticatedUser(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
    }

    private void logLines(java.util.List<String> logLines) {
        for (String logLine : logLines) {
            log.info("{}", logLine);
        }
    }

}
