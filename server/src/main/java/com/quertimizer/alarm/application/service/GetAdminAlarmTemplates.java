package com.quertimizer.alarm.application.service;

import com.quertimizer.alarm.application.port.in.GetAdminAlarmTemplatesUseCase;
import com.quertimizer.alarm.application.output.AlarmTemplateOutput;
import com.quertimizer.alarm.application.port.out.AlarmTemplateRepositoryPort;
import com.quertimizer.alarm.application.service.AlarmTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetAdminAlarmTemplates implements GetAdminAlarmTemplatesUseCase {

    private final AlarmTemplateRepositoryPort alarmTemplateRepository;
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
    @Override
    public List<AlarmTemplateOutput> execute() {
        alarmTemplateService.ensureDefaultTemplates();
        return alarmTemplateRepository.findAllByOrderByAlarmTypeAsc().stream()
                .map(alarmTemplateService::toAlarmTemplateOutput)
                .toList();
    }
}
