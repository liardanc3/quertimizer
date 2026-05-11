package com.quertimizer.alarm.application.input;

import lombok.Data;

@Data
public class AlarmPageInput {

    private final String handle;
    private final int page;
    private final Integer pageSize;
    private final String createdAtSort;
}
