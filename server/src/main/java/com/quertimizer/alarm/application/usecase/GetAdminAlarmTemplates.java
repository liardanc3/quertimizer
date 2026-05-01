package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.output.AlarmTemplateOutput;
import com.quertimizer.alarm.application.port.AlarmTemplateRepository;
import com.quertimizer.alarm.application.service.AlarmTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetAdminAlarmTemplates {

    private final AlarmTemplateRepository alarmTemplateRepository;
    private final AlarmTemplateService alarmTemplateService;

    /**
     * 관리자 알람 템플릿 목록을 조회한다.
     *
     * <ol>
     *   <li>기본 알람 템플릿 보장
     *   <li>알람 템플릿 목록 조회와 응답 변환
     * </ol>
     */
    @Transactional
    public List<AlarmTemplateOutput> execute() {
        alarmTemplateService.ensureDefaultTemplates();
        return alarmTemplateRepository.findAllByOrderByAlarmTypeAsc().stream()
                .map(alarmTemplateService::toAlarmTemplateOutput)
                .toList();
    }
}
