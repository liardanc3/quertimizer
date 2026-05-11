package com.quertimizer.alarm.application.output;

import lombok.Data;

@Data
public class AlarmTemplateOutput {

    private final String type;
    private final String sentence;
    private final String description;
}
