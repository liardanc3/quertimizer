package com.quertimizer.user.application.service;

import com.quertimizer.user.application.port.in.UnblockIpUseCase;
import com.quertimizer.auth.application.port.out.BlockedIpRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UnblockIp implements UnblockIpUseCase {

    private final BlockedIpRepositoryPort blockedIpRepository;

    /**
     * IP 차단을 해제한다.
     *
     * @param ipAddress 차단 해제할 IP 주소
     */
    @Transactional
    @Override
    public void execute(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        blockedIpRepository.deleteById(ipAddress.trim());
    }
}
