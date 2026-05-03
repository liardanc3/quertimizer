package com.quertimizer.alarm.adapter.out.external;

import com.quertimizer.alarm.application.port.out.AlarmNotifierPort;
import com.quertimizer.alarm.application.output.AlarmCreatedOutput;
import com.quertimizer.alarm.adapter.in.web.realtime.dto.AlarmSocketRes;
import com.quertimizer.global.realtime.sender.SessionStompSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionAlarmNotifier implements AlarmNotifierPort {

    private final SessionStompSender sessionStompSender;

    @Override
    public void notifyCreated(String handle, AlarmCreatedOutput payload) throws Exception {
        sessionStompSender.sendToUser(handle, AlarmSocketRes.from(payload));
    }
}
