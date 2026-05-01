package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.port.BlockedIpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UnblockIp {

    private final BlockedIpRepository blockedIpRepository;

    /**
     * IP 차단을 해제한다.
     *
     * @param ipAddress 차단 해제할 IP 주소
     */
    @Transactional
    public void execute(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        blockedIpRepository.deleteById(ipAddress.trim());
    }
}
