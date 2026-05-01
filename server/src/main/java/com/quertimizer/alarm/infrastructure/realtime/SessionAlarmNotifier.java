package com.quertimizer.alarm.infrastructure.realtime;

import com.quertimizer.alarm.application.port.AlarmNotifier;
import com.quertimizer.alarm.application.output.AlarmCreatedOutput;
import com.quertimizer.alarm.presentation.realtime.dto.AlarmSocketRes;
import com.quertimizer.global.realtime.sender.SessionStompSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionAlarmNotifier implements AlarmNotifier {

    private final SessionStompSender sessionStompSender;

    /**
     * 생성된 알람 payload를 사용자 STOMP 세션으로 전송한다.
     *
     * @param handle 알람을 받을 사용자 handle
     * @param payload 전송할 알람 생성 payload
     * @throws Exception STOMP 전송에 실패한 경우
     */
    @Override
    public void notifyCreated(String handle, AlarmCreatedOutput payload) throws Exception {
        sessionStompSender.sendToUser(handle, AlarmSocketRes.from(payload));
    }
}
