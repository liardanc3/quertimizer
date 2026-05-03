package com.quertimizer.alarm.application.service;

import com.quertimizer.alarm.application.output.AlarmTemplateOutput;
import com.quertimizer.alarm.application.port.out.AlarmTemplateRepositoryPort;
import com.quertimizer.alarm.domain.entity.AlarmTemplate;
import com.quertimizer.alarm.domain.model.AlarmTemplateDefault;
import com.quertimizer.alarm.domain.model.AlarmTemplateDefaults;
import com.quertimizer.alarm.domain.model.AlarmType;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.quertimizer.alarm.domain.model.AlarmTemplateFailReason.ALARM_TEMPLATE_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class AlarmTemplateService {

    private final AlarmTemplateRepositoryPort alarmTemplateRepository;

    @Transactional
    public void ensureDefaultTemplates() {
        // 저장된 알람 템플릿 조회
        Map<String, AlarmTemplate> savedTemplatesByType = alarmTemplateRepository.findAllByOrderByAlarmTypeAsc().stream()
                .collect(Collectors.toMap(AlarmTemplate::getAlarmType, Function.identity()));

        // 누락된 기본 템플릿 저장
        for (Map.Entry<String, AlarmTemplateDefault> defaultTemplateEntry : AlarmTemplateDefaults.VALUES.entrySet()) {
            if (savedTemplatesByType.containsKey(defaultTemplateEntry.getKey())) {
                continue;
            }

            alarmTemplateRepository.save(AlarmTemplate.create(
                    defaultTemplateEntry.getKey(),
                    defaultTemplateEntry.getValue().getSentence(),
                    defaultTemplateEntry.getValue().getDescription()
            ));
        }
    }

    public AlarmTemplate getAlarmTemplate(String alarmType) {
        // 기본 알람 템플릿 보장 후 유형 정규화
        ensureDefaultTemplates();
        String normalizedAlarmType = normalizeAlarmType(alarmType);

        // 알람 유형 기준 템플릿 조회
        return alarmTemplateRepository.findById(normalizedAlarmType)
                .orElseThrow(() -> new BusinessException(ALARM_TEMPLATE_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
    }

    public String requireText(String value, String message) {
        // 텍스트 필수값 검증
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }

        return value.trim();
    }

    public AlarmTemplateOutput toAlarmTemplateOutput(AlarmTemplate alarmTemplate) {
        // 알람 템플릿 응답으로 변환
        return new AlarmTemplateOutput(
                alarmTemplate.getAlarmType(),
                alarmTemplate.getSentence(),
                alarmTemplate.getDescription()
        );
    }

    private String normalizeAlarmType(String alarmType) {
        // 알람 유형 공백 여부 검사
        if (alarmType == null || alarmType.isBlank()) {
            return alarmType;
        }

        // legacy 알람 유형을 현재 알람 유형으로 변환
        return switch (alarmType) {
            case "community_post_like" -> AlarmType.LIKE_MY_POST.getValue();
            case "community_post_comment" -> AlarmType.COMMENT_MY_POST.getValue();
            case "community_comment_reply" -> AlarmType.REPLY_MY_COMMENT.getValue();
            case "community_comment_like" -> AlarmType.LIKE_MY_COMMENT.getValue();
            default -> alarmType;
        };
    }

}
