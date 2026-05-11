package com.quertimizer.alarm.application.output;

import lombok.Data;

@Data
public class AlarmCreatedOutput {

    private final String type;
    private final AlarmItemOutput alarm;
    private final long unreadCount;

    public static AlarmCreatedOutput created(AlarmItemOutput alarm, long unreadCount) {
        return new AlarmCreatedOutput("alarm.created", alarm, unreadCount);
    }
}
