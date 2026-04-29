package com.quertimizer.auth.presentation.support;

import com.quertimizer.global.realtime.sender.SessionSocketSender;
import com.quertimizer.global.support.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
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
    private final SessionSocketSender sessionSocketSender;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    /**
     * 로그인 성공 인증 결과로 remember-me 쿠키를 저장한다.
     *
     * @param authentication 쿠키에 반영할 인증 결과
     * @param httpRequest remember-me 처리에 사용하는 HTTP 요청
     * @param httpResponse remember-me 쿠키를 기록할 HTTP 응답
     */
    public void saveRememberMeCookie(Authentication authentication,
                                     HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        rememberMeServices.loginSuccess(httpRequest, httpResponse, authentication);
    }

    /**
     * remember-me 쿠키와 SecurityContext 로그아웃 상태를 함께 정리한다.
     *
     * @param authentication 정리할 인증 정보
     * @param httpRequest 로그아웃 처리에 사용하는 HTTP 요청
     * @param httpResponse remember-me 쿠키 삭제에 사용하는 HTTP 응답
     */
    public void deleteRememberMeCookie(Authentication authentication,
                                       HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        rememberMeServices.logout(httpRequest, httpResponse, authentication);
        new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, authentication);
    }

    /**
     * HTTP 세션에 연결된 WebSocket 연결을 종료한다.
     *
     * @param sessionId 종료할 HTTP 세션 ID
     */
    public void closeSessionSocket(String sessionId) {
        sessionSocketSender.closeSessionSockets(sessionId);
    }

    /**
     * 인증 결과를 현재 스레드와 Spring Security 인증 저장소에 반영한다.
     *
     * <ol>
     *   <li>SecurityContext 생성
     *   <li>현재 스레드의 SecurityContextHolder에 반영
     *   <li>SecurityContextRepository에 저장
     * </ol>
     *
     * @param authentication 저장할 인증 결과
     * @param httpRequest 인증 저장소 저장에 사용하는 HTTP 요청
     * @param httpResponse 인증 저장소 저장에 사용하는 HTTP 응답
     */
    public void saveAuthenticationToRepository(Authentication authentication,
                                               HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);

        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);
    }

    /**
     * provider 정보를 포함한 소셜 로그인 성공 URL을 만든다.
     *
     * @param provider 성공한 소셜 로그인 provider
     */
    public String buildSocialLoginSuccessUrl(String provider) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replaceQueryParam("socialLoginSuccess", provider)
                .build()
                .toUriString();
    }

    /**
     * provider 정보를 포함한 소셜 로그인 실패 URL을 만든다.
     *
     * @param provider 실패한 소셜 로그인 provider
     */
    public String buildSocialLoginFailureUrl(String provider) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replaceQueryParam("socialLoginError", provider == null || provider.isBlank() ? "oauth2" : provider)
                .build()
                .toUriString();
    }

    /**
     * 프록시 헤더와 remote address에서 클라이언트 IP를 결정한다.
     *
     * @param httpRequest 클라이언트 IP를 확인할 HTTP 요청
     */
    public String resolveClientIp(HttpServletRequest httpRequest) {
        return clientIpResolver.resolve(httpRequest);
    }
}
