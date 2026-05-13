package com.quertimizer.alarm.adapter.out.websocket.response;

import com.quertimizer.alarm.application.output.AlarmCreatedOutput;
import com.quertimizer.alarm.application.output.AlarmItemOutput;
import lombok.Data;

@Data
public class AlarmSocketRes {

    private final String type;
    private final AlarmItemOutput alarm;
    private final long unreadCount;

    public static AlarmSocketRes created(AlarmItemOutput alarm, long unreadCount) {
        return new AlarmSocketRes("alarm.created", alarm, unreadCount);
    }

    public static AlarmSocketRes from(AlarmCreatedOutput result) {
        return new AlarmSocketRes(result.getType(), result.getAlarm(), result.getUnreadCount());
    }
}
