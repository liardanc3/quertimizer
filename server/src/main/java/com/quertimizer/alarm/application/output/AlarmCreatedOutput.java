package com.quertimizer.alarm.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlarmCreatedOutput {

    private final String type;
    private final AlarmItemOutput alarm;
    private final long unreadCount;

    public static AlarmCreatedOutput created(AlarmItemOutput alarm, long unreadCount) {
        // 생성 결과 반환
        return new AlarmCreatedOutput("alarm.created", alarm, unreadCount);
    }
}
