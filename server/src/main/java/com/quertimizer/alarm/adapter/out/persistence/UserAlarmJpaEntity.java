package com.quertimizer.alarm.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_alarm")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAlarmJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_id", nullable = false)
    private Long alarmId;

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    @Column(name = "alarm_type", nullable = false, length = 100)
    private String alarmType;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "target_path", nullable = false, length = 255)
    private String targetPath;

    @Column(name = "target_hash", length = 255)
    private String targetHash;

    @Column(name = "bindings_json", columnDefinition = "TEXT")
    private String bindingsJson;

    @Column(nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static UserAlarmJpaEntity create(String handle, String alarmType, String title, String message,
                                            String targetPath, String targetHash, String bindingsJson,
                                            boolean read, LocalDateTime createdAt) {
        // 사용자 알람 JPA 엔티티 생성
        return new UserAlarmJpaEntity(
                null, handle, alarmType, title, message,
                targetPath, targetHash, bindingsJson, read, createdAt
        );
    }

    public void update(boolean read) {
        // 사용자 알람 읽음 상태 변경
        this.read = read;
    }

    private UserAlarmJpaEntity(Long alarmId, String handle, String alarmType, String title, String message,
                               String targetPath, String targetHash, String bindingsJson,
                               boolean read, LocalDateTime createdAt) {
        this.alarmId = alarmId;
        this.handle = handle;
        this.alarmType = alarmType;
        this.title = title;
        this.message = message;
        this.targetPath = targetPath;
        this.targetHash = targetHash;
        this.bindingsJson = bindingsJson;
        this.read = read;
        this.createdAt = createdAt;
    }
}
