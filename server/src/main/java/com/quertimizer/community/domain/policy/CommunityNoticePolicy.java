package com.quertimizer.community.domain.policy;

import com.quertimizer.user.domain.model.UserRole;
import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import org.springframework.stereotype.Component;

import static com.quertimizer.community.domain.model.CommunityFailReason.NOTICE_WRITE_DENIED;
import static com.quertimizer.community.domain.model.CommunityPostConstant.NOTICE_CATEGORY;

@Component
public class CommunityNoticePolicy {

    public void validateNoticeWritable(UserRole role, String currentCategory, String nextCategory) {
        // 공지 카테고리 변경 대상 여부 확인
        if (!NOTICE_CATEGORY.equals(currentCategory) && !NOTICE_CATEGORY.equals(nextCategory)) {
            return;
        }

        // 관리자 역할 여부 검증
        if (role != UserRole.ADMIN) {
            throw new DomainRuleViolationException(NOTICE_WRITE_DENIED.getMessage(), DomainRuleViolationType.ACCESS_DENIED);
        }
    }
}
