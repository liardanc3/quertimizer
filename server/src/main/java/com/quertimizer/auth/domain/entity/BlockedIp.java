package com.quertimizer.auth.domain.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BlockedIp {

    private String ipAddress;
    private String blockedHandle;
    private LocalDateTime blockedAt;

    public static BlockedIp create(String ipAddress, String blockedHandle) {
        // 차단 IP 신규 저장용 엔티티 생성
        return new BlockedIp(ipAddress.trim(), blockedHandle, LocalDateTime.now());
    }

    public static BlockedIp restore(String ipAddress, String blockedHandle, LocalDateTime blockedAt) {
        // 저장된 차단 IP 상태 복원
        return new BlockedIp(ipAddress, blockedHandle, blockedAt);
    }

    public void refresh(String blockedHandle) {
        // 같은 IP 재차단 시 대상 handle과 시각 최신화
        this.blockedHandle = blockedHandle;
        this.blockedAt = LocalDateTime.now();
    }

    private BlockedIp(String ipAddress, String blockedHandle, LocalDateTime blockedAt) {
        this.ipAddress = ipAddress;
        this.blockedHandle = blockedHandle;
        this.blockedAt = blockedAt;
    }
}
