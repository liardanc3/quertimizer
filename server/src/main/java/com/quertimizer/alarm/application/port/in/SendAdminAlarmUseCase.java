package com.quertimizer.alarm.application.port.in;

import com.quertimizer.alarm.application.input.SendAdminAlarmInput;
import com.quertimizer.alarm.domain.model.AdminDirectAlarm;
import com.quertimizer.user.domain.entity.User;

public interface SendAdminAlarmUseCase {

    int execute(SendAdminAlarmInput input);
}
