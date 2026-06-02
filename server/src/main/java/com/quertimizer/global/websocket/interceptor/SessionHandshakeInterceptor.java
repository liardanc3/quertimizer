package com.quertimizer.global.websocket.interceptor;

import com.quertimizer.auth.application.port.in.ResolveAuthenticatedHandleUseCase;
import com.quertimizer.auth.application.port.in.ValidateAuthenticatedUserAccessUseCase;
import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.log.LogFormatter;
import com.quertimizer.global.log.LogMdcContext;
import com.quertimizer.global.util.ClientIpResolver;
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
    private final ResolveAuthenticatedHandleUseCase resolveAuthenticatedHandle;
    private final ValidateAuthenticatedUserAccessUseCase validateAuthenticatedUserAccess;
    private final ClientIpResolver clientIpResolver;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String actor = resolveActor(request);

        try (LogMdcContext.LogActorScope ignored = LogMdcContext.openActorScope(actor)) {
            log.info("WebSocket handshake request");
            logLines(logFormatter.formatQueryStringLines("", request.getURI().getQuery()));
        }

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

        String userIdentifier = resolveHandshakeUserIdentifier(authentication);

        if (session == null) {
            session = httpServletRequest.getSession(true);
        }

        attributes.put("handle", userIdentifier);
        attributes.put("sessionId", session.getId());
        attributes.put("clientIp", clientIpResolver.resolve(httpServletRequest));
        attributes.put("admin", isAdmin(authentication));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception == null) {
            try (LogMdcContext.LogActorScope ignored = LogMdcContext.openActorScope(resolveActor(request))) {
                log.info("WebSocket handshake success");
            }
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
            return resolveAuthenticatedActor(authentication.getName());
        }

        return httpServletRequest.getRemoteAddr();
    }

    private String resolveAuthenticatedActor(String email) {
        // handle 조회 성공 시 handle 우선 사용
        String resolvedHandle = resolveHandleQuietly(email);
        if (resolvedHandle != null && !resolvedHandle.isBlank()) {
            return resolvedHandle;
        }

        return email;
    }

    private String resolveHandleQuietly(String email) {
        // handle 조회 실패 시 email fallback
        try {
            return resolveAuthenticatedHandle.execute(email);
        } catch (RuntimeException exception) {
            return "";
        }
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
            validateAuthenticatedUserAccess.execute(authentication.getName());
            return false;
        } catch (DomainRuleViolationException exception) {
            return true;
        }
    }

    private boolean isAdmin(Authentication authentication) {
        // 관리자 권한 여부 확인
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String resolveHandshakeUserIdentifier(Authentication authentication) {
        // handle 존재 시 WebSocket 사용자 식별자로 사용
        String resolvedHandle = resolveAuthenticatedHandle.execute(authentication.getName());
        if (resolvedHandle != null && !resolvedHandle.isBlank()) {
            return resolvedHandle;
        }

        // handle 미설정 사용자는 인증 이메일을 WebSocket 사용자 식별자로 사용
        return authentication.getName();
    }

    private void logLines(java.util.List<String> logLines) {
        // 로그 라인을 순서대로 기록
        for (String logLine : logLines) {
            log.info("{}", logLine);
        }
    }
}
