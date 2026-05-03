package com.quertimizer.auth.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_ip")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockedIpJpaEntity {

    @Id
    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(name = "blocked_handle", length = 50)
    private String blockedHandle;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    public static BlockedIpJpaEntity create(String ipAddress, String blockedHandle, LocalDateTime blockedAt) {
        // 차단 IP JPA 엔티티 생성
        return new BlockedIpJpaEntity(ipAddress, blockedHandle, blockedAt);
    }

    public void update(String blockedHandle, LocalDateTime blockedAt) {
        // 차단 IP 대상 handle과 시각 변경
        this.blockedHandle = blockedHandle;
        this.blockedAt = blockedAt;
    }

    private BlockedIpJpaEntity(String ipAddress, String blockedHandle, LocalDateTime blockedAt) {
        this.ipAddress = ipAddress;
        this.blockedHandle = blockedHandle;
        this.blockedAt = blockedAt;
    }
}
