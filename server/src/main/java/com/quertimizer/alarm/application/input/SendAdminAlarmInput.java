package com.quertimizer.alarm.application.input;

import lombok.Data;

import java.util.List;

@Data
public class SendAdminAlarmInput {

    private final List<String> recipientHandles;
    private final String message;
}
