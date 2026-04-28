package com.quertimizer.alarm.application.service;

import com.quertimizer.alarm.application.input.AlarmTemplateInput;
import com.quertimizer.alarm.application.output.AlarmTemplateOutput;
import com.quertimizer.alarm.domain.entity.AlarmTemplate;
import com.quertimizer.alarm.domain.model.AlarmType;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.alarm.application.port.AlarmTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.quertimizer.alarm.domain.model.AlarmTemplateFailReason.ALARM_TEMPLATE_NOT_FOUND;
import static com.quertimizer.alarm.domain.model.AlarmTemplateFailReason.DESCRIPTION_REQUIRED;
import static com.quertimizer.alarm.domain.model.AlarmTemplateFailReason.SENTENCE_REQUIRED;

@Service
@RequiredArgsConstructor
@Transactional
public class AlarmTemplateService {

    private static final Map<String, DefaultAlarmTemplate> DEFAULT_ALARM_TEMPLATES = new LinkedHashMap<>();

    static {
        DEFAULT_ALARM_TEMPLATES.put(AlarmType.LIKE_MY_POST.getValue(), new DefaultAlarmTemplate(
                AlarmType.LIKE_MY_POST.getDefaultSentence(),
                AlarmType.LIKE_MY_POST.getDefaultDescription()
        ));
        DEFAULT_ALARM_TEMPLATES.put(AlarmType.COMMENT_MY_POST.getValue(), new DefaultAlarmTemplate(
                AlarmType.COMMENT_MY_POST.getDefaultSentence(),
                AlarmType.COMMENT_MY_POST.getDefaultDescription()
        ));
        DEFAULT_ALARM_TEMPLATES.put(AlarmType.REPLY_MY_COMMENT.getValue(), new DefaultAlarmTemplate(
                AlarmType.REPLY_MY_COMMENT.getDefaultSentence(),
                AlarmType.REPLY_MY_COMMENT.getDefaultDescription()
        ));
        DEFAULT_ALARM_TEMPLATES.put(AlarmType.LIKE_MY_COMMENT.getValue(), new DefaultAlarmTemplate(
                AlarmType.LIKE_MY_COMMENT.getDefaultSentence(),
                AlarmType.LIKE_MY_COMMENT.getDefaultDescription()
        ));
    }

    private final AlarmTemplateRepository alarmTemplateRepository;

    @Transactional
    public void ensureDefaultTemplates() {
        // 기본 알람 템플릿을 보장
        Map<String, AlarmTemplate> savedTemplatesByType = alarmTemplateRepository.findAllByOrderByAlarmTypeAsc().stream()
                .collect(Collectors.toMap(AlarmTemplate::getAlarmType, Function.identity()));

        for (Map.Entry<String, DefaultAlarmTemplate> defaultTemplateEntry : DEFAULT_ALARM_TEMPLATES.entrySet()) {
            if (savedTemplatesByType.containsKey(defaultTemplateEntry.getKey())) {
                continue;
            }

            alarmTemplateRepository.save(AlarmTemplate.create(
                    defaultTemplateEntry.getKey(),
                    defaultTemplateEntry.getValue().sentence(),
                    defaultTemplateEntry.getValue().description()
            ));
        }
    }

    public List<AlarmTemplateOutput> getAdminAlarmTemplates() {
        // 관리자 알람 템플릿 목록을 조회
        ensureDefaultTemplates();
        return alarmTemplateRepository.findAllByOrderByAlarmTypeAsc().stream()
                .map(this::toAlarmTemplateOutput)
                .toList();
    }

    public AlarmTemplate getAlarmTemplate(String alarmType) {
        // 알람 타입 기준 템플릿을 조회
        ensureDefaultTemplates();
        String normalizedAlarmType = normalizeAlarmType(alarmType);

        return alarmTemplateRepository.findById(normalizedAlarmType)
                .orElseThrow(() -> new BusinessException(ALARM_TEMPLATE_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
    }

    @Transactional
    public AlarmTemplateOutput updateAlarmTemplate(AlarmTemplateInput input) {
        // 알람 템플릿 내용을 수정
        AlarmTemplate alarmTemplate = getAlarmTemplate(input.getAlarmType());

        alarmTemplate.changeContent(
                requireText(input.getSentence(), SENTENCE_REQUIRED.getMessage()),
                requireText(input.getDescription(), DESCRIPTION_REQUIRED.getMessage())
        );
        return toAlarmTemplateOutput(alarmTemplate);
    }

    private String normalizeAlarmType(String alarmType) {
        // 알람 유형 정규화
        if (alarmType == null || alarmType.isBlank()) {
            return alarmType;
        }

        return switch (alarmType) {
            case "community_post_like" -> AlarmType.LIKE_MY_POST.getValue();
            case "community_post_comment" -> AlarmType.COMMENT_MY_POST.getValue();
            case "community_comment_reply" -> AlarmType.REPLY_MY_COMMENT.getValue();
            case "community_comment_like" -> AlarmType.LIKE_MY_COMMENT.getValue();
            default -> alarmType;
        };
    }

    private String requireText(String value, String message) {
        // 텍스트 필수값 검증
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }

        return value.trim();
    }

    private AlarmTemplateOutput toAlarmTemplateOutput(AlarmTemplate alarmTemplate) {
        // 알람 템플릿 응답으로 변환
        return new AlarmTemplateOutput(
                alarmTemplate.getAlarmType(),
                alarmTemplate.getSentence(),
                alarmTemplate.getDescription()
        );
    }

    private record DefaultAlarmTemplate(String sentence, String description) {
    }

}
