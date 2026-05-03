package com.quertimizer.alarm.application.port.in;

import com.quertimizer.alarm.domain.entity.UserAlarm;

public interface MarkAllAlarmsReadUseCase {

    void execute(String handle);
}
