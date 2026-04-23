package com.quertimizer.alarm.domain.model;

public enum AlarmLogMessage {

    SOCKET_SEND_FAILED("알람 소켓 전송에 실패했다."),
    BINDING_SERIALIZE_FAILED("알람 바인딩 직렬화에 실패했다."),
    BINDING_DESERIALIZE_FAILED("알람 바인딩 역직렬화에 실패했다.");

    private final String message;

    AlarmLogMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
