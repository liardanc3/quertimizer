package com.quertimizer.community.domain.policy;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.application.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityNoticePolicy {

    private final UserRepository userRepository;

    /**
     * 공지 카테고리 게시글 작성 또는 수정 권한을 검증한다.
     *
     * @param handle 게시글 작성 또는 수정 사용자 handle
     * @param currentCategory 현재 게시글 카테고리
     * @param nextCategory 변경하려는 게시글 카테고리
     */
    public void validateNoticeWritable(String handle, String currentCategory, String nextCategory) {
        if (!"notice".equals(currentCategory) && !"notice".equals(nextCategory)) {
            return;
        }

        userRepository.findByHandle(handle)
                .filter(user -> user.getResolvedRole() == UserRole.ADMIN)
                .orElseThrow(() -> new BusinessException("공지 게시글은 관리자만 작성하거나 수정할 수 있습니다.", HttpStatus.FORBIDDEN));
    }
}
