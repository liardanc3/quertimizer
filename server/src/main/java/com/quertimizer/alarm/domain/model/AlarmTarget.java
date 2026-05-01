package com.quertimizer.alarm.domain.model;

import lombok.Value;

@Value
public class AlarmTarget {

    String path;
    String hash;

    public AlarmTarget(String path, String hash) {
        this.path = path;
        this.hash = hash;
    }

    public static AlarmTarget of(String path) {
        return new AlarmTarget(path, null);
    }

    public static AlarmTarget of(String path, String hash) {
        return new AlarmTarget(path, hash);
    }

    public String path() {
        return path;
    }

    public String hash() {
        return hash;
    }

}
