package com.quertimizer.alarm.application.port.in;

import com.quertimizer.alarm.application.input.AlarmTemplateInput;
import com.quertimizer.alarm.application.output.AlarmTemplateOutput;
import com.quertimizer.alarm.domain.entity.AlarmTemplate;

public interface UpdateAlarmTemplateUseCase {

    AlarmTemplateOutput execute(AlarmTemplateInput input);
}
