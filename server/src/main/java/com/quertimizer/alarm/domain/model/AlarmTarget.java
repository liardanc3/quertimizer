package com.quertimizer.alarm.domain.model;

public record AlarmTarget(String path, String hash) {

    public static AlarmTarget of(String path) {
        // 알람 이동 대상 생성
        return new AlarmTarget(path, null);
    }

    public static AlarmTarget of(String path, String hash) {
        // 알람 이동 대상 생성
        return new AlarmTarget(path, hash);
    }

}
