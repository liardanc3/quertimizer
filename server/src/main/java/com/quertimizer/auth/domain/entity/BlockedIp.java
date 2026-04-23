package com.quertimizer.auth.domain.entity;

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
public class BlockedIp {

    @Id
    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(name = "blocked_handle", length = 50)
    private String blockedHandle;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    public static BlockedIp create(String ipAddress, String blockedHandle) {
        return new BlockedIp(ipAddress.trim(), blockedHandle, LocalDateTime.now());
    }

    public void refresh(String blockedHandle) {
        this.blockedHandle = blockedHandle;
        this.blockedAt = LocalDateTime.now();
    }

    private BlockedIp(String ipAddress, String blockedHandle, LocalDateTime blockedAt) {
        this.ipAddress = ipAddress;
        this.blockedHandle = blockedHandle;
        this.blockedAt = blockedAt;
    }

}
