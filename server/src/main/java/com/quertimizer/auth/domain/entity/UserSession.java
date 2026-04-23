package com.quertimizer.auth.domain.entity;

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

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    public static UserSession create(String sessionId, String handle) {
        // 새 로그인 세션이나 remember-me 복구 세션을 저장할 때 사용한다.
        return new UserSession(sessionId, handle, LocalDateTime.now());
    }

    public void refresh(String handle) {
        // 같은 세션이 다시 저장되면 사용자와 저장 시각을 최신 상태로 갱신한다.
        this.handle = handle;
        this.savedAt = LocalDateTime.now();
    }

    public String sessionId() {
        return sessionId;
    }

    public String handle() {
        return handle;
    }

    private UserSession(String sessionId, String handle, LocalDateTime savedAt) {
        this.sessionId = sessionId;
        this.handle = handle;
        this.savedAt = savedAt;
    }

}
