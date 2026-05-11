package com.quertimizer.alarm.application.port.in;

import com.quertimizer.alarm.domain.model.AlarmSpec;

public interface PublishAlarmUseCase {

    void execute(AlarmSpec alarmSpec);

}
