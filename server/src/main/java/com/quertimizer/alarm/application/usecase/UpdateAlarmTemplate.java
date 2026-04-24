package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.input.AlarmTemplateInput;
import com.quertimizer.alarm.application.output.AlarmTemplateOutput;
import com.quertimizer.alarm.application.service.AlarmTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateAlarmTemplate {

    private final AlarmTemplateService alarmTemplateService;

    public AlarmTemplateOutput execute(String alarmType, AlarmTemplateInput input) {
        // 관리자 알람 템플릿을 수정
        return alarmTemplateService.updateAlarmTemplate(alarmType, input);
    }
}
