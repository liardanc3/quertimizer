package com.quertimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSession {

    @Id
    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    public static UserSession create(String sessionId, String userId) {
        return new UserSession(sessionId, userId, LocalDateTime.now());
    }

    public void refresh(String userId) {
        this.userId = userId;
        this.savedAt = LocalDateTime.now();
    }

    public String sessionId() {
        return sessionId;
    }

    public String userId() {
        return userId;
    }

    private UserSession(String sessionId, String userId, LocalDateTime savedAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.savedAt = savedAt;
    }

}
