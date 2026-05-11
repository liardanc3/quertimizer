package com.quertimizer.community.adapter.in.web.support;

import com.quertimizer.auth.application.port.in.ResolveAuthenticatedHandleUseCase;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import com.quertimizer.global.constant.GlobalFailReason;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class CommunitySupport {

    private final ResolveAuthenticatedHandleUseCase resolveAuthenticatedHandle;

    public String resolveCurrentHandle(Authentication authentication) {
        // Spring Security 인증 정보 기준 현재 사용자 handle 확인
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return resolveAuthenticatedHandle.execute(authentication.getName());
    }

    public String requireCurrentHandle(Authentication authentication) {
        // Spring Security 인증 정보 기준 현재 사용자 handle 확인
        String currentHandle = resolveCurrentHandle(authentication);
        if (currentHandle == null || currentHandle.isBlank()) {
            throw new BusinessException(GlobalFailReason.AUTHENTICATION_REQUIRED.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        // 인증 handle 반환
        return currentHandle;
    }

    public URI buildPostLocation(Long postId) {
        // 생성된 게시글 Location 응답 URI 생성
        return URI.create("/community/posts/" + CommunityPostIdPolicy.format(postId));
    }
}
