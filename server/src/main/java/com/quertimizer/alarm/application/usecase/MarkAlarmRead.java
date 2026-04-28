package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.input.MarkAlarmReadInput;
import com.quertimizer.alarm.application.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkAlarmRead {

    private final AlarmService alarmService;

    /**
     * 사용자 단일 알람을 읽음 처리한다.
     *
     * @param input 읽음 처리할 알람과 사용자 조건
     */
    public boolean execute(MarkAlarmReadInput input) {
        return alarmService.markRead(input);
    }
}
