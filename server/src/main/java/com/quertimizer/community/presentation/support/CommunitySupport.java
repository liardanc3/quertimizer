package com.quertimizer.community.presentation.support;

import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class CommunitySupport {

    private final AuthService authService;

    public String resolveCurrentHandle(Authentication authentication) {
        // Spring Security 인증 정보 기준 현재 사용자 handle 확인
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.resolveCurrentHandle(authentication.getName());
    }

    public URI buildPostLocation(Long postId) {
        // 생성된 게시글 Location 응답 URI 생성
        return URI.create("/community/posts/" + CommunityPostIdPolicy.format(postId));
    }
}
