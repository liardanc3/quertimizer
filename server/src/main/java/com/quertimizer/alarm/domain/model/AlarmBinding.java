package com.quertimizer.alarm.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class AlarmBinding {

    String text;
    String path;
    String hash;

    @JsonCreator
    public AlarmBinding(@JsonProperty("text") String text,
                        @JsonProperty("path") String path,
                        @JsonProperty("hash") String hash) {
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
