package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkAlarmRead {

    private final AlarmService alarmService;

    public boolean execute(Long alarmId, String handle) {
        // 단일 알람을 읽음 처리
        return alarmService.markRead(alarmId, handle);
    }
}
