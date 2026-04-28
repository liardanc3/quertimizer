package com.quertimizer.alarm.application.input;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SendAdminAlarmInput {

    private final List<String> recipientHandles;
    private final String message;
}
