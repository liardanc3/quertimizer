package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.ValidateClientIpAccessUseCase;
import com.quertimizer.auth.application.port.out.BlockedIpRepositoryPort;
import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import com.quertimizer.global.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.auth.domain.model.AuthFailReason.BLOCKED_IP;

@Component
@RequiredArgsConstructor
public class ValidateClientIpAccess implements ValidateClientIpAccessUseCase {

    private final BlockedIpRepositoryPort blockedIpRepository;

    /**
     * 클라이언트 IP 접근 가능 여부를 검증한다.
     *
     * @param clientIp 현재 요청의 클라이언트 IP
     */
    @Override
    @Transactional(readOnly = true)
    public void execute(String clientIp) {
        // 빈 IP는 차단 대상 조회 없이 허용
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }

        // 차단된 IP 요청이면 접근 차단
        if (blockedIpRepository.existsByIpAddress(clientIp.trim())) {
            throw new DomainRuleViolationException(BLOCKED_IP.getMessage(), DomainRuleViolationType.ACCESS_DENIED);
        }
    }
}
