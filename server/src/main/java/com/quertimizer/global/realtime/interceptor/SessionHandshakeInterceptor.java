package com.quertimizer.global.realtime.interceptor;

import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.log.LogFormatter;
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
    private final AuthService authService;
    private final LoginPolicy loginPolicy;

    /**
     * WebSocket handshake 요청을 인증하고 세션 속성을 준비한다.
     *
     * <ol>
     *   <li>handshake 요청 로그 기록
     *   <li>HTTP 세션 또는 principal 인증 정보 확인
     *   <li>차단 사용자 검증
     *   <li>WebSocket 세션 속성 저장
     * </ol>
     *
     * @param request handshake HTTP 요청
     * @param response handshake HTTP 응답
     * @param wsHandler WebSocket handler
     * @param attributes WebSocket 세션에 전달할 속성 map
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String actor = resolveActor(request);
        String prefix = logFormatter.prefix(actor);

        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket handshake request", null));
        logLines(logFormatter.formatQueryStringLines(prefix, request.getURI().getQuery()));

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

        if (isBlockedUser(authentication)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        String resolvedHandle = authService.resolveCurrentHandle(authentication.getName());
        if (resolvedHandle == null || resolvedHandle.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (session == null) {
            session = httpServletRequest.getSession(true);
        }

        attributes.put("handle", resolvedHandle);
        attributes.put("sessionId", session.getId());
        return true;
    }

    /**
     * WebSocket handshake 성공 로그를 기록한다.
     *
     * @param request handshake HTTP 요청
     * @param response handshake HTTP 응답
     * @param wsHandler WebSocket handler
     * @param exception handshake 처리 예외
     */
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
        // WebSocket handshake 로그 주체를 결정
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
        // HttpSession 또는 Servlet principal에서 Authentication을 조회
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
        // 인증된 일반 사용자 여부를 확인
        return authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
    }

    private boolean isBlockedUser(Authentication authentication) {
        // 차단된 사용자 여부를 확인
        try {
            loginPolicy.validateBlockedUser(authentication.getName());
            return false;
        } catch (BusinessException exception) {
            return true;
        }
    }

    private void logLines(java.util.List<String> logLines) {
        // 로그 라인을 순서대로 기록
        for (String logLine : logLines) {
            log.info("{}", logLine);
        }
    }
}
