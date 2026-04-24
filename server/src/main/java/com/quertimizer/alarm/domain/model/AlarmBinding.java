package com.quertimizer.alarm.domain.model;

public record AlarmBinding(String text, String path, String hash) {

    public static AlarmBinding of(String text, String path) {
        // 알람 바인딩 생성
        return new AlarmBinding(text, path, null);
    }

    public static AlarmBinding of(String text, String path, String hash) {
        // 알람 바인딩 생성
        return new AlarmBinding(text, path, hash);
    }

}
