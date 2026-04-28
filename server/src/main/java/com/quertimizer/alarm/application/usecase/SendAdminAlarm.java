package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.input.SendAdminAlarmInput;
import com.quertimizer.alarm.application.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendAdminAlarm {

    private final AlarmService alarmService;

    /**
     * 관리자 공지 알람을 전송한다.
     *
     * @param input 관리자 알람 수신자와 메시지
     */
    public int execute(SendAdminAlarmInput input) {
        return alarmService.sendAdminAlarm(input);
    }
}
