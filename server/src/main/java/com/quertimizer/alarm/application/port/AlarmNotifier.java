package com.quertimizer.alarm.application.port;

import com.quertimizer.alarm.application.output.AlarmCreatedOutput;

public interface AlarmNotifier {

    void notifyCreated(String handle, AlarmCreatedOutput payload) throws Exception;
}
