package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.output.AlarmPageOutput;
import com.quertimizer.alarm.application.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetAlarms {

    private final AlarmService alarmService;

    public AlarmPageOutput execute(String handle, int page, Integer pageSize) {
        // 사용자 알람 목록을 조회
        return alarmService.getAlarms(handle, page, pageSize);
    }
}
