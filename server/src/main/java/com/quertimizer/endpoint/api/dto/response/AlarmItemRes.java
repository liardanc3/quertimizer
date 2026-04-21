package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.alarm.AlarmBinding;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class AlarmItemRes {

    private final Long alarmId;
    private final String alarmType;
    private final String title;
    private final String message;
    private final String sentence;
    private final String description;
    private final Map<String, AlarmBinding> bindings;
    private final String targetPath;
    private final String targetHash;
    private final boolean read;
    private final LocalDateTime createdAt;

}
