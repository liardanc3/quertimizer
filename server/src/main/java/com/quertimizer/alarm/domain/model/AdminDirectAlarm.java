package com.quertimizer.alarm.domain.model;

import java.util.Map;

public record AdminDirectAlarm(String recipientHandle, String message) implements AlarmSpec {

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
