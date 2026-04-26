package com.quertimizer.alarm.infrastructure.realtime;

import com.quertimizer.alarm.application.port.AlarmNotifier;
import com.quertimizer.alarm.application.output.AlarmCreatedOutput;
import com.quertimizer.alarm.presentation.realtime.dto.AlarmSocketRes;
import com.quertimizer.global.realtime.sender.SessionSocketSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionAlarmNotifier implements AlarmNotifier {

    private final SessionSocketSender sessionSocketSender;

    @Override
    public void notifyCreated(String handle, AlarmCreatedOutput payload) throws Exception {
        // 생성된 알람을 소켓으로 전송
        sessionSocketSender.sendToUser(handle, AlarmSocketRes.from(payload));
    }
}
