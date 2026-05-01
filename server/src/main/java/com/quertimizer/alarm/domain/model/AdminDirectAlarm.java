package com.quertimizer.alarm.domain.model;

import lombok.Value;

import java.util.Map;

@Value
public class AdminDirectAlarm implements AlarmSpec {

    String recipientHandle;
    String message;

    public AdminDirectAlarm(String recipientHandle, String message) {
        this.recipientHandle = recipientHandle;
        this.message = message;
    }

    @Override
    public String recipientHandle() {
        return recipientHandle;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public String alarmType() {
        return AlarmType.FROM_ADMIN.getValue();
    }

    @Override
    public String title() {
        return AlarmType.FROM_ADMIN.getTitle();
    }

    @Override
    public AlarmTarget target() {
        return AlarmTarget.of("");
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        return Map.of();
    }
}
