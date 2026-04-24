package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.output.BlockedUserPageOutput;
import com.quertimizer.auth.application.service.AccountRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetBlockedUsers {

    private final AccountRestrictionService accountRestrictionService;

    public BlockedUserPageOutput execute(int page, Integer pageSize) {
        // 차단된 사용자 목록을 조회
        return accountRestrictionService.getBlockedUsers(page, pageSize);
    }
}
