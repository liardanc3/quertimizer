package com.quertimizer.user.application.service;

import com.quertimizer.user.application.port.in.UnblockIpUseCase;
import com.quertimizer.user.application.port.out.UserAccountRestrictionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UnblockIp implements UnblockIpUseCase {

    private final UserAccountRestrictionPort userAccountRestrictionPort;

    /**
     * IP 차단을 해제한다.
     *
     * @param ipAddress 차단 해제할 IP 주소
     */
    @Transactional
    @Override
    public void execute(String ipAddress) {
        userAccountRestrictionPort.unblockIp(ipAddress);
    }
}
