package com.quertimizer.community.presentation.support;

import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class CommunitySupport {

    private final AuthService authService;

    public String resolveCurrentHandle(Authentication authentication) {
        // 현재 인증 기준 Handle을 해석
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.resolveCurrentHandle(authentication.getName());
    }

    public URI buildPostLocation(String postId) {
        // 게시글 상세 URI를 생성
        return URI.create("/community/posts/" + postId);
    }
}
