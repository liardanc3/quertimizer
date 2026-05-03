package com.quertimizer.alarm.adapter.in.web.realtime.dto;

import com.quertimizer.alarm.application.output.AlarmCreatedOutput;
import com.quertimizer.alarm.adapter.in.web.response.AlarmItemRes;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlarmSocketRes {

    private final String type;
    private final AlarmItemRes alarm;
    private final long unreadCount;

    public static AlarmSocketRes created(AlarmItemRes alarm, long unreadCount) {
        return new AlarmSocketRes("alarm.created", alarm, unreadCount);
    }

    public static AlarmSocketRes from(AlarmCreatedOutput result) {
        return new AlarmSocketRes(
                result.getType(),
                AlarmItemRes.from(result.getAlarm()),
                result.getUnreadCount()
        );
    }
}
