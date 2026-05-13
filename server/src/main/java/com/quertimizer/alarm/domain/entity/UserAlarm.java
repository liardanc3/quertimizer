package com.quertimizer.alarm.domain.entity;

import com.quertimizer.alarm.domain.model.AlarmSpec;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserAlarm {

    private Long alarmId;
    private String handle;
    private String alarmType;
    private String title;
    private String message;
    private String targetPath;
    private String targetHash;
    private String bindingsJson;
    private boolean read;
    private LocalDateTime createdAt;

    public static UserAlarm create(AlarmSpec alarmSpec, String bindingsJson) {
        return new UserAlarm(
                alarmSpec.recipientHandle(), alarmSpec.alarmType(),
                alarmSpec.title(), alarmSpec.message(),
                alarmSpec.target().path(), alarmSpec.target().hash(),
                bindingsJson, false, LocalDateTime.now()
        );
    }

    public static UserAlarm restore(Long alarmId, String handle, String alarmType, String title,
                                    String message, String targetPath, String targetHash, String bindingsJson,
                                    boolean read, LocalDateTime createdAt) {
        // 저장된 사용자 알람 상태 복원
        UserAlarm userAlarm = new UserAlarm(
                handle, alarmType, title, message, targetPath,
                targetHash, bindingsJson, read, createdAt
        );
        userAlarm.alarmId = alarmId;
        return userAlarm;
    }

    public void markRead() {
        this.read = true;
    }

    private UserAlarm(String handle, String alarmType, String title, String message,
                      String targetPath, String targetHash, String bindingsJson,
                      boolean read, LocalDateTime createdAt) {
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
