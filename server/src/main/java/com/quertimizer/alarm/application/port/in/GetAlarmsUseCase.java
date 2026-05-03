package com.quertimizer.alarm.application.port.in;

import com.quertimizer.alarm.application.input.AlarmPageInput;
import com.quertimizer.alarm.application.output.AlarmPageOutput;

public interface GetAlarmsUseCase {

    AlarmPageOutput execute(AlarmPageInput input);
}
