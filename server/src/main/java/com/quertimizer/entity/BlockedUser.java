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
@Table(name = "blocked_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockedUser {

    @Id
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "last_access_ip", length = 64)
    private String lastAccessIp;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    public static BlockedUser create(String userId, String lastAccessIp) {
        return new BlockedUser(userId, lastAccessIp, LocalDateTime.now());
    }

    public void refresh(String lastAccessIp) {
        this.lastAccessIp = normalizeIp(lastAccessIp);
        this.blockedAt = LocalDateTime.now();
    }

    private BlockedUser(String userId, String lastAccessIp, LocalDateTime blockedAt) {
        this.userId = userId;
        this.lastAccessIp = normalizeIp(lastAccessIp);
        this.blockedAt = blockedAt;
    }

    private static String normalizeIp(String lastAccessIp) {
        if (lastAccessIp == null || lastAccessIp.isBlank()) {
            return null;
        }

        return lastAccessIp.trim();
    }

}
