package com.quertimizer.alarm.domain.model;

import java.util.Map;

public record AdminDirectAlarm(String recipientHandle, String message) implements AlarmSpec {

    @Override
    public String alarmType() {
        // 관리자 알람 유형 반환
        return AlarmType.FROM_ADMIN.getValue();
    }

    @Override
    public String title() {
        // 관리자 알람 제목 반환
        return AlarmType.FROM_ADMIN.getTitle();
    }

    @Override
    public AlarmTarget target() {
        // 관리자 알람 이동 대상 반환
        return AlarmTarget.of("");
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        // 관리자 알람 바인딩 반환
        return Map.of();
    }
}
