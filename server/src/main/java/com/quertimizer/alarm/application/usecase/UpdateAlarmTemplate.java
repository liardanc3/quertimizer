package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.input.AlarmTemplateInput;
import com.quertimizer.alarm.application.output.AlarmTemplateOutput;
import com.quertimizer.alarm.application.service.AlarmTemplateService;
import com.quertimizer.alarm.domain.entity.AlarmTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.alarm.domain.model.AlarmTemplateFailReason.DESCRIPTION_REQUIRED;
import static com.quertimizer.alarm.domain.model.AlarmTemplateFailReason.SENTENCE_REQUIRED;

@Component
@RequiredArgsConstructor
public class UpdateAlarmTemplate {

    private final AlarmTemplateService alarmTemplateService;

    /**
     * 관리자 알람 템플릿 내용을 수정한다.
     *
     * <ol>
     *   <li>수정 대상 알람 템플릿 조회
     *   <li>템플릿 문장과 설명 변경
     *   <li>알람 템플릿 응답 변환
     * </ol>
     *
     * @param input 수정할 알람 템플릿 내용
     */
    @Transactional
    public AlarmTemplateOutput execute(AlarmTemplateInput input) {
        AlarmTemplate alarmTemplate = alarmTemplateService.getAlarmTemplate(input.getAlarmType());
        alarmTemplate.changeContent(
                alarmTemplateService.requireText(input.getSentence(), SENTENCE_REQUIRED.getMessage()),
                alarmTemplateService.requireText(input.getDescription(), DESCRIPTION_REQUIRED.getMessage())
        );
        return alarmTemplateService.toAlarmTemplateOutput(alarmTemplate);
    }
}
