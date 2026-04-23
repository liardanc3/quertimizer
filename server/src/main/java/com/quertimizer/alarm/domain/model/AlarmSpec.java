package com.quertimizer.alarm.domain.model;

import java.util.Map;

public interface AlarmSpec {

    String recipientHandle();

    String alarmType();

    String title();

    String message();

    AlarmTarget target();

    Map<String, AlarmBinding> bindings();

}
