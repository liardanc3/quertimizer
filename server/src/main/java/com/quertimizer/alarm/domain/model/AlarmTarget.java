package com.quertimizer.alarm.domain.model;

public record AlarmTarget(String path, String hash) {

    public static AlarmTarget of(String path) {
        return new AlarmTarget(path, null);
    }

    public static AlarmTarget of(String path, String hash) {
        return new AlarmTarget(path, hash);
    }

}
