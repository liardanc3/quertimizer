package com.quertimizer.alarm.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlarmTemplateOutput {

    private final String type;
    private final String sentence;
    private final String description;
}
