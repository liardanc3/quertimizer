package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.output.BlockedIpPageOutput;
import com.quertimizer.auth.application.service.AccountRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetBlockedIps {

    private final AccountRestrictionService accountRestrictionService;

    public BlockedIpPageOutput execute(int page, Integer pageSize) {
        // 차단된 IP 목록을 조회
        return accountRestrictionService.getBlockedIps(page, pageSize);
    }
}
