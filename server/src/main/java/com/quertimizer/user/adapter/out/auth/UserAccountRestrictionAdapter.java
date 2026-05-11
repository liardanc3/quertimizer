package com.quertimizer.user.adapter.out.auth;

import com.quertimizer.auth.application.port.out.BlockedIpRepositoryPort;
import com.quertimizer.auth.domain.entity.BlockedIp;
import com.quertimizer.user.application.port.out.UserAccountRestrictionPort;
import com.quertimizer.user.domain.model.UserBlockedIp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAccountRestrictionAdapter implements UserAccountRestrictionPort {

    private final BlockedIpRepositoryPort blockedIpRepository;

    @Override
    public void blockIp(String ipAddress, String handle) {
        // IP 값 없으면 차단 IP 저장 생략
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        // 기존 차단 IP는 대상 handle과 차단 시각 갱신
        String normalizedIpAddress = ipAddress.trim();
        blockedIpRepository.findById(normalizedIpAddress)
                .ifPresentOrElse(
                        blockedIp -> {
                            blockedIp.refresh(handle);
                            blockedIpRepository.save(blockedIp);
                        },
                        () -> blockedIpRepository.save(BlockedIp.create(normalizedIpAddress, handle))
                );
    }

    @Override
    public void unblockHandle(String handle) {
        // handle 기준 연결된 차단 IP 제거
        blockedIpRepository.deleteByBlockedHandle(handle);
    }

    @Override
    public void unblockIp(String ipAddress) {
        // IP 값 없으면 차단 해제 생략
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        // 정규화된 IP 기준 차단 제거
        blockedIpRepository.deleteById(ipAddress.trim());
    }

    @Override
    public Page<UserBlockedIp> findBlockedIps(Pageable pageable) {
        // auth 차단 IP 저장소 결과를 user 조회 모델로 변환
        return blockedIpRepository.findAllByOrderByBlockedAtDescIpAddressAsc(pageable)
                .map(blockedIp -> new UserBlockedIp(blockedIp.getIpAddress(), blockedIp.getBlockedAt()));
    }
}
