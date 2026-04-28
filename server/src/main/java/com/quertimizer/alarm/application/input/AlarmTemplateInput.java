package com.quertimizer.alarm.application.input;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlarmTemplateInput {

    private final String alarmType;
    private final String sentence;
    private final String description;
}
