package com.quertimizer.alarm;

public record AlarmBinding(String text, String path, String hash) {

    public static AlarmBinding of(String text, String path) {
        return new AlarmBinding(text, path, null);
    }

    public static AlarmBinding of(String text, String path, String hash) {
        return new AlarmBinding(text, path, hash);
    }

}
