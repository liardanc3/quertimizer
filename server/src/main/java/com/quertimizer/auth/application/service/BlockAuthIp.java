package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.input.AuthIpBlockInput;
import com.quertimizer.auth.application.port.in.BlockAuthIpUseCase;
import com.quertimizer.auth.application.port.out.BlockedIpRepositoryPort;
import com.quertimizer.auth.domain.entity.BlockedIp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BlockAuthIp implements BlockAuthIpUseCase {

    private final BlockedIpRepositoryPort blockedIpRepository;

    /**
     * 사용자 마지막 접속 IP를 차단한다.
     *
     * <ol>
     *   <li>IP 값 정규화
     *   <li>기존 차단 IP 갱신 또는 신규 저장
     * </ol>
     *
     * @param input 차단할 IP와 연결 handle
     */
    @Override
    @Transactional
    public void execute(AuthIpBlockInput input) {
        if (input.getIpAddress() == null || input.getIpAddress().isBlank()) {
            return;
        }

        String normalizedIpAddress = input.getIpAddress().trim();
        blockedIpRepository.findById(normalizedIpAddress)
                .ifPresentOrElse(
                        blockedIp -> {
                            blockedIp.refresh(input.getHandle());
                            blockedIpRepository.save(blockedIp);
                        },
                        () -> blockedIpRepository.save(BlockedIp.create(normalizedIpAddress, input.getHandle()))
                );
    }
}
