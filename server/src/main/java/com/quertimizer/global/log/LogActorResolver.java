package com.quertimizer.global.log;

import com.quertimizer.auth.application.port.in.ResolveAuthenticatedHandleUseCase;
import com.quertimizer.global.support.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LogActorResolver {

    private final ClientIpResolver clientIpResolver;
    private final ResolveAuthenticatedHandleUseCase resolveAuthenticatedHandle;

    public String resolve(HttpServletRequest request, Authentication authentication) {
        // 인증정보 없으면 클라이언트 IP 사용
        String clientIp = clientIpResolver.resolve(request);
        if (!isAuthenticatedUser(authentication)) {
            return clientIp;
        }

        // handle, email, IP 순서로 로그 주체 결정
        return resolveAuthenticatedActor(authentication, clientIp);
    }

    private String resolveAuthenticatedActor(Authentication authentication, String fallbackActor) {
        // 인증 email 없으면 fallback 주체 사용
        String email = resolveAuthenticatedEmail(authentication);
        if (email == null || email.isBlank()) {
            return fallbackActor;
        }

        // handle이 있으면 handle 우선 사용
        String resolvedHandle = resolveHandleQuietly(email);
        if (resolvedHandle != null && !resolvedHandle.isBlank()) {
            return resolvedHandle;
        }

        return email;
    }

    private String resolveHandleQuietly(String email) {
        // 로그 주체 조회 실패 시 요청 흐름을 막지 않고 email fallback
        try {
            return resolveAuthenticatedHandle.execute(email);
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        // UserDetails principal에서 email 추출
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        // OAuth2 principal에서 email 추출
        if (principal instanceof OAuth2User oAuth2User) {
            String oauth2Email = resolveOAuth2Email(oAuth2User.getAttributes());
            if (oauth2Email != null && !oauth2Email.isBlank()) {
                return oauth2Email;
            }
        }

        // 인증 이름이 email 형태면 그대로 사용
        String authenticationName = authentication.getName();
        return authenticationName != null && authenticationName.contains("@") ? authenticationName : "";
    }

    private String resolveOAuth2Email(Map<String, Object> attributes) {
        // 표준 email attribute 조회
        String email = resolveText(attributes.get("email"));
        if (email != null && !email.isBlank()) {
            return email;
        }

        // Kakao 계정 email attribute 조회
        Object kakaoAccount = attributes.get("kakao_account");
        if (kakaoAccount instanceof Map<?, ?> accountAttributes) {
            return resolveText(accountAttributes.get("email"));
        }

        return "";
    }

    private String resolveText(Object value) {
        // 문자열 값 여부 확인
        return value instanceof String text && !text.isBlank() ? text : "";
    }

    private boolean isAuthenticatedUser(Authentication authentication) {
        // 인증된 일반 사용자 여부 확인
        return authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
    }
}
