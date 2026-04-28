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

    /**
     * 관리자 알람 템플릿 내용을 수정한다.
     *
     * @param input 수정할 알람 템플릿 내용
     */
    public AlarmTemplateOutput execute(AlarmTemplateInput input) {
        return alarmTemplateService.updateAlarmTemplate(input);
    }
}
