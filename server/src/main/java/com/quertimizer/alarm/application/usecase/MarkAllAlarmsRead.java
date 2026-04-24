package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkAllAlarmsRead {

    private final AlarmService alarmService;

    public void execute(String handle) {
        // 사용자 알람을 모두 읽음 처리
        alarmService.markAllRead(handle);
    }
}
