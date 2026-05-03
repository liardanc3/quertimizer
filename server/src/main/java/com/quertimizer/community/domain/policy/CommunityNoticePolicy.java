package com.quertimizer.community.domain.policy;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;

public class CommunityNoticePolicy {

    public void validateNoticeWritable(UserRole role, String currentCategory, String nextCategory) {
        // 공지 카테고리 변경 대상 여부 확인
        if (!"notice".equals(currentCategory) && !"notice".equals(nextCategory)) {
            return;
        }

        // 관리자 역할 여부 검증
        if (role != UserRole.ADMIN) {
            throw new DomainRuleViolationException(
                    "공지 게시글은 관리자만 작성하거나 수정할 수 있습니다.",
                    DomainRuleViolationType.ACCESS_DENIED
            );
        }
    }
}
