package com.quertimizer.alarm.application.port.in;

import com.quertimizer.alarm.application.output.AlarmTemplateOutput;
import java.util.List;

public interface GetAdminAlarmTemplatesUseCase {

    List<AlarmTemplateOutput> execute();
}
