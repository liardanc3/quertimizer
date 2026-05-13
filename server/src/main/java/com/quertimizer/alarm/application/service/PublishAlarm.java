package com.quertimizer.alarm.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.alarm.application.port.in.PublishAlarmUseCase;
import com.quertimizer.alarm.domain.model.AlarmSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PublishAlarm implements PublishAlarmUseCase {

    private final AlarmService alarmService;

    /**
     * 알람 명세에 맞는 알람을 발행한다.
     *
     * @param alarmSpec 발행할 알람 명세
     */
    @Transactional
    @Override
    @Log("알람 발행")
    public void execute(AlarmSpec alarmSpec) {
        alarmService.publish(alarmSpec);
    }

}
