package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.service.AccountRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnblockIp {

    private final AccountRestrictionService accountRestrictionService;

    /**
     * IP 차단을 해제한다.
     *
     * @param ipAddress 차단 해제할 IP 주소
     */
    public void execute(String ipAddress) {
        accountRestrictionService.unblockIp(ipAddress);
    }
}
