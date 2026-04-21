package com.quertimizer.endpoint.websocket.dto;

import com.quertimizer.endpoint.api.dto.response.AlarmItemRes;
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

}
