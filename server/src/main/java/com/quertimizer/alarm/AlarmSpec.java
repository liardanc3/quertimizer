package com.quertimizer.alarm;

import java.util.Map;

public interface AlarmSpec {

    String recipientUserId();

    String alarmType();

    String title();

    String message();

    AlarmTarget target();

    Map<String, AlarmBinding> bindings();

}
