package com.quertimizer.alarm.domain.model;

import lombok.Value;

@Value
public class AlarmBinding {

    String text;
    String path;
    String hash;

    public AlarmBinding(String text, String path, String hash) {
        this.text = text;
        this.path = path;
        this.hash = hash;
    }

    public static AlarmBinding of(String text, String path) {
        return new AlarmBinding(text, path, null);
    }

    public static AlarmBinding of(String text, String path, String hash) {
        return new AlarmBinding(text, path, hash);
    }

    public String text() {
        return text;
    }

    public String path() {
        return path;
    }

    public String hash() {
        return hash;
    }

}
