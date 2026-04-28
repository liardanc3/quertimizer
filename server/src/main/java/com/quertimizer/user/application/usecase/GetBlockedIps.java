package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.output.BlockedIpPageOutput;
import com.quertimizer.auth.application.service.AccountRestrictionService;
import com.quertimizer.user.application.input.BlockedAccountPageInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetBlockedIps {

    private final AccountRestrictionService accountRestrictionService;

    /**
     * 차단된 IP 목록을 조회한다.
     *
     * @param input 차단 IP 페이지 조회 입력
     */
    public BlockedIpPageOutput execute(BlockedAccountPageInput input) {
        return accountRestrictionService.getBlockedIps(input.getPage(), input.getPageSize());
    }
}
