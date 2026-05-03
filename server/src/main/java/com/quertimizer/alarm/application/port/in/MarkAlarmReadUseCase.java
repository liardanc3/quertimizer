package com.quertimizer.alarm.application.port.in;

import com.quertimizer.alarm.application.input.MarkAlarmReadInput;
import com.quertimizer.alarm.domain.entity.UserAlarm;

public interface MarkAlarmReadUseCase {

    boolean execute(MarkAlarmReadInput input);
}
