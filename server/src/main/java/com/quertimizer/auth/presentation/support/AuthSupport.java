package com.quertimizer.auth.presentation.support;

import com.quertimizer.global.util.CanonicalCode;
import com.quertimizer.problem.presentation.realtime.handler.SessionWebSocketHandler;
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
    private final SessionWebSocketHandler sessionWebSocketHandler;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public void saveRememberMeCookie(Authentication authentication,
                                     HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // remember-me 쿠키를 응답에 기록
        rememberMeServices.loginSuccess(httpRequest, httpResponse, authentication);
    }

    public void deleteRememberMeCookie(Authentication authentication,
                                       HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // remember-me 쿠키와 인증 세션을 함께 정리
        rememberMeServices.logout(httpRequest, httpResponse, authentication);
        new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, authentication);
    }

    public void closeSessionSocket(String sessionId) {
        // 세션에 연결된 소켓 종료
        sessionWebSocketHandler.closeSessionSockets(sessionId);
    }

    @CanonicalCode
    public void saveAuthenticationToRepository(Authentication authentication,
                                               HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // SecurityContext 생성 후 인증결과 주입
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);

        // SecurityContext를 현재 스레드의 SecurityContextHolder에 반영
        SecurityContextHolder.setContext(securityContext);

        // SecurityContext를 인증 저장소에 저장
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);
    }

    public String buildSocialLoginSuccessUrl(String provider) {
        // 프런트가 provider별 성공 후처리를 할 수 있게 query param 유지
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replaceQueryParam("socialLoginSuccess", provider)
                .build()
                .toUriString();
    }

    public String buildSocialLoginFailureUrl(String provider) {
        // provider를 모를 때도 프런트가 공통 에러문구를 만들 수 있도록 oauth2 기본값 사용
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replaceQueryParam("socialLoginError", provider == null || provider.isBlank() ? "oauth2" : provider)
                .build()
                .toUriString();
    }

    public String resolveClientIp(HttpServletRequest httpRequest) {
        // 프록시 환경에서는 X-Forwarded-For의 첫 번째 값을 실제 접속 IP로 사용
        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        // 프록시 정보가 없으면 직접 연결된 remote address 사용
        return httpRequest.getRemoteAddr();
    }
}
