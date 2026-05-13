package com.quertimizer.alarm.adapter.out.websocket.handler;

import com.quertimizer.alarm.application.port.out.AlarmNotifierPort;
import com.quertimizer.alarm.application.output.AlarmCreatedOutput;
import com.quertimizer.alarm.adapter.out.websocket.response.AlarmSocketRes;
import com.quertimizer.global.websocket.sender.WebSocketSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlarmHandler implements AlarmNotifierPort {

    private final WebSocketSender webSocketSender;

    @Override
    public void notifyCreated(String handle, AlarmCreatedOutput payload) throws Exception {
        // 알림 생성 웹소켓 응답 전송
        webSocketSender.sendToUser(handle, AlarmSocketRes.from(payload));
    }
}
