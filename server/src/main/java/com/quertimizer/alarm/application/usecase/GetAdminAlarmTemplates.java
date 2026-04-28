package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.output.AlarmTemplateOutput;
import com.quertimizer.alarm.application.service.AlarmTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetAdminAlarmTemplates {

    private final AlarmTemplateService alarmTemplateService;

    /**
     * 관리자 알람 템플릿 목록을 조회한다.
     */
    public List<AlarmTemplateOutput> execute() {
        return alarmTemplateService.getAdminAlarmTemplates();
    }
}
