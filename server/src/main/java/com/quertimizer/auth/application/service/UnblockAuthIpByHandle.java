package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.UnblockAuthIpByHandleUseCase;
import com.quertimizer.auth.application.port.out.BlockedIpRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UnblockAuthIpByHandle implements UnblockAuthIpByHandleUseCase {

    private final BlockedIpRepositoryPort blockedIpRepository;

    /**
     * 사용자 handle과 연결된 IP 차단을 해제한다.
     *
     * @param handle 차단 해제 대상 사용자 handle
     */
    @Override
    @Transactional
    public void execute(String handle) {
        blockedIpRepository.deleteByBlockedHandle(handle);
    }
}
