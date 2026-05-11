package com.quertimizer.alarm.application.input;

import lombok.Data;

@Data
public class AlarmTemplateInput {

    private final String alarmType;
    private final String sentence;
    private final String description;
}
