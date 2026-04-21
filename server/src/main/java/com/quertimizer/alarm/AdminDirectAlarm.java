package com.quertimizer.alarm;

import java.util.Map;

public record AdminDirectAlarm(String recipientUserId, String message) implements AlarmSpec {

    private static final String ALARM_TYPE = "FROM_ADMIN";
    private static final String TITLE = "관리자 알람";

    @Override
    public String alarmType() {
        return ALARM_TYPE;
    }

    @Override
    public String title() {
        return TITLE;
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
