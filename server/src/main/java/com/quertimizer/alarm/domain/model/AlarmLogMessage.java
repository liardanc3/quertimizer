package com.quertimizer.alarm.domain.model;

import lombok.Getter;

@Getter
public enum AlarmLogMessage {

    SOCKET_SEND_FAILED("알람 소켓 전송 실패"),
    BINDING_SERIALIZE_FAILED("알람 바인딩 직렬화 실패"),
    BINDING_DESERIALIZE_FAILED("알람 바인딩 역직렬화 실패");

    private final String message;

    AlarmLogMessage(String message) {
        this.message = message;
    }

}
