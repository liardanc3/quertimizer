package com.quertimizer.entity;

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

    @Column(name = "blocked_user_id", length = 50)
    private String blockedUserId;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    public static BlockedIp create(String ipAddress, String blockedUserId) {
        return new BlockedIp(ipAddress.trim(), blockedUserId, LocalDateTime.now());
    }

    public void refresh(String blockedUserId) {
        this.blockedUserId = blockedUserId;
        this.blockedAt = LocalDateTime.now();
    }

    private BlockedIp(String ipAddress, String blockedUserId, LocalDateTime blockedAt) {
        this.ipAddress = ipAddress;
        this.blockedUserId = blockedUserId;
        this.blockedAt = blockedAt;
    }

}
