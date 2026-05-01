package com.quertimizer.auth.domain.entity;

import com.quertimizer.user.domain.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_ip")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockedIp {

    @Id
    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(name = "blocked_handle", length = 50)
    private String blockedHandle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_handle", referencedColumnName = "handle", insertable = false, updatable = false)
    private User blockedUser;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    public static BlockedIp create(String ipAddress, String blockedHandle) {
        // 차단 IP 신규 저장용 엔티티 생성
        return new BlockedIp(ipAddress.trim(), blockedHandle, LocalDateTime.now());
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
