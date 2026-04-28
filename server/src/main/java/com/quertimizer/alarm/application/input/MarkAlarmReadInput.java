package com.quertimizer.alarm.application.input;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarkAlarmReadInput {

    private final Long alarmId;
    private final String handle;
}
