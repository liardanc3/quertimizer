package com.quertimizer.auth.adapter.in.http.support;

import com.quertimizer.auth.application.output.AuthenticatedUserOutput;
import com.quertimizer.global.websocket.sender.WebSocketSender;
import com.quertimizer.global.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class AuthSupport {

    private final TokenBasedRememberMeServices rememberMeServices;
    private final SecurityContextRepository securityContextRepository;
    private final WebSocketSender webSocketSender;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public void saveRememberMeCookie(Authentication authentication,
                                     HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 로그인 성공 인증 결과로 remember-me 쿠키 저장
        rememberMeServices.loginSuccess(httpRequest, httpResponse, authentication);
    }

    public void saveRememberMeCookie(AuthenticatedUserOutput authenticatedUser,
                                     HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 인증 사용자 정보로 remember-me 쿠키 저장
        rememberMeServices.loginSuccess(httpRequest, httpResponse, createAuthentication(authenticatedUser));
    }

    public void deleteRememberMeCookie(Authentication authentication,
                                       HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // remember-me 쿠키와 SecurityContext 로그아웃 상태 정리
        rememberMeServices.logout(httpRequest, httpResponse, authentication);
        new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, authentication);
    }

    public void closeSessionWebSockets(String sessionId) {
        // HTTP 세션에 연결된 WebSocket 세션 종료
        webSocketSender.closeHttpSessionWebSockets(sessionId);
    }

    public void saveAuthenticationToRepository(Authentication authentication,
                                               HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // SecurityContext 생성과 인증 결과 반영
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);

        // 현재 스레드의 SecurityContextHolder 반영
        SecurityContextHolder.setContext(securityContext);

        // SecurityContextRepository 저장
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);
    }

    public void saveAuthenticationToRepository(AuthenticatedUserOutput authenticatedUser,
                                               HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 인증 사용자 정보로 SecurityContext 저장
        saveAuthenticationToRepository(createAuthentication(authenticatedUser), httpRequest, httpResponse);
    }

    public Authentication createAuthentication(AuthenticatedUserOutput authenticatedUser) {
        // 인증 사용자 정보를 Spring Security 인증 객체로 변환
        String authority = "ROLE_" + authenticatedUser.getRole().name();
        return UsernamePasswordAuthenticationToken.authenticated(
                new org.springframework.security.core.userdetails.User(
                        authenticatedUser.getEmail(), authenticatedUser.getPassword(), AuthorityUtils.createAuthorityList(authority)
                ),
                authenticatedUser.getPassword(),
                AuthorityUtils.createAuthorityList(authority)
        );
    }

    public String buildSocialLoginSuccessUrl(String provider) {
        // provider 정보를 포함한 소셜 로그인 성공 URL 생성
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replaceQueryParam("socialLoginSuccess", provider)
                .build()
                .toUriString();
    }

    public String buildSocialLoginFailureUrl(String provider) {
        // provider 정보를 포함한 소셜 로그인 실패 URL 생성
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replaceQueryParam("socialLoginError", provider == null || provider.isBlank() ? "oauth2" : provider)
                .build()
                .toUriString();
    }

    public String resolveClientIp(HttpServletRequest httpRequest) {
        // 프록시 헤더와 remote address 기준 클라이언트 IP 결정
        return clientIpResolver.resolve(httpRequest);
    }
}
