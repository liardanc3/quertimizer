package com.quertimizer.alarm.application.port.out;

import com.quertimizer.alarm.application.output.AlarmCreatedOutput;

public interface AlarmNotifierPort {

    void notifyCreated(String handle, AlarmCreatedOutput payload) throws Exception;
}
