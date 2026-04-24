package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.service.AccountRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnblockIp {

    private final AccountRestrictionService accountRestrictionService;

    public void execute(String ipAddress) {
        // IP 차단을 해제
        accountRestrictionService.unblockIp(ipAddress);
    }
}
