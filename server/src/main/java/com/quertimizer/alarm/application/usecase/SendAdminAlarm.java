package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SendAdminAlarm {

    private final AlarmService alarmService;

    public int execute(List<String> recipientHandles, String message) {
        // 관리자 공지 알람을 전송
        return alarmService.sendAdminAlarm(recipientHandles, message);
    }
}
