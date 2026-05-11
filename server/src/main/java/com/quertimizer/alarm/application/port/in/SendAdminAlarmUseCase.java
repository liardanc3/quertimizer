package com.quertimizer.alarm.application.port.in;

import com.quertimizer.alarm.application.input.SendAdminAlarmInput;

public interface SendAdminAlarmUseCase {

    int execute(SendAdminAlarmInput input);
}
