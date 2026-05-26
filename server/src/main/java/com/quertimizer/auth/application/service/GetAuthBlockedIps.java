package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.output.AuthBlockedIpOutput;
import com.quertimizer.auth.application.port.in.GetAuthBlockedIpsUseCase;
import com.quertimizer.auth.application.port.out.BlockedIpRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetAuthBlockedIps implements GetAuthBlockedIpsUseCase {

    private final BlockedIpRepositoryPort blockedIpRepository;

    /**
     * 차단 IP 목록을 조회한다.
     *
     * @param pageable 차단 IP 페이지 조건
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AuthBlockedIpOutput> execute(Pageable pageable) {
        return blockedIpRepository.findAllByOrderByBlockedAtDescIpAddressAsc(pageable)
                .map(blockedIp -> new AuthBlockedIpOutput(blockedIp.getIpAddress(), blockedIp.getBlockedAt()));
    }
}
