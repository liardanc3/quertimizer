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

    /**
     * Spring Security 인증 정보에서 현재 사용자 handle을 확인한다.
     *
     * @param authentication 현재 요청의 인증 정보
     */
    public String resolveCurrentHandle(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.resolveCurrentHandle(authentication.getName());
    }

    /**
     * 생성된 게시글의 Location 응답 URI를 만든다.
     *
     * @param postId Location으로 변환할 게시글 번호
     */
    public URI buildPostLocation(Long postId) {
        return URI.create("/community/posts/" + CommunityPostIdPolicy.format(postId));
    }
}
