package com.quertimizer.alarm.application.input;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlarmPageInput {

    private final String handle;
    private final int page;
    private final Integer pageSize;
    private final String createdAtSort;
}
