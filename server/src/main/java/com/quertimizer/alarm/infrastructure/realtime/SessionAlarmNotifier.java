package com.quertimizer.alarm.infrastructure.realtime;

import com.quertimizer.alarm.application.port.AlarmNotifier;
import com.quertimizer.alarm.application.output.AlarmCreatedOutput;
import com.quertimizer.alarm.presentation.realtime.dto.AlarmSocketRes;
import com.quertimizer.problem.presentation.realtime.handler.SessionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionAlarmNotifier implements AlarmNotifier {

    private final SessionWebSocketHandler sessionWebSocketHandler;

    @Override
    public void notifyCreated(String handle, AlarmCreatedOutput payload) throws Exception {
        // 생성된 알람을 소켓으로 전송
        sessionWebSocketHandler.sendAlarm(handle, AlarmSocketRes.from(payload));
    }
}
