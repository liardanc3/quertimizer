package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.input.AlarmPageInput;
import com.quertimizer.alarm.application.output.AlarmPageOutput;
import com.quertimizer.alarm.application.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetAlarms {

    private final AlarmService alarmService;

    /**
     * 사용자 알람 페이지를 조회한다.
     *
     * @param input 알람 페이지 조회 조건
     */
    public AlarmPageOutput execute(AlarmPageInput input) {
        return alarmService.getAlarms(input);
    }
}
