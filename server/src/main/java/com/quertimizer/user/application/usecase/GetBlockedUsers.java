package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.output.BlockedUserPageOutput;
import com.quertimizer.auth.application.service.AccountRestrictionService;
import com.quertimizer.user.application.input.BlockedAccountPageInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetBlockedUsers {

    private final AccountRestrictionService accountRestrictionService;

    /**
     * 차단된 사용자 목록을 조회한다.
     *
     * @param input 차단 사용자 페이지 조회 입력
     */
    public BlockedUserPageOutput execute(BlockedAccountPageInput input) {
        return accountRestrictionService.getBlockedUsers(input.getPage(), input.getPageSize());
    }
}
