package com.quertimizer.alarm.application.input;

import lombok.Data;

@Data
public class MarkAlarmReadInput {

    private final Long alarmId;
    private final String handle;
}
