package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkAllAlarmsRead {

    private final AlarmService alarmService;

    /**
     * 사용자 알람을 모두 읽음 처리한다.
     *
     * @param handle 읽음 처리할 사용자 handle
     */
    public void execute(String handle) {
        alarmService.markAllRead(handle);
    }
}
