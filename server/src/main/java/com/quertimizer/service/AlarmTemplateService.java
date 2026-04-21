package com.quertimizer.service;

import com.quertimizer.endpoint.api.dto.request.AlarmTemplateSaveReq;
import com.quertimizer.endpoint.api.dto.response.AlarmTemplateRes;
import com.quertimizer.entity.AlarmTemplate;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.repository.AlarmTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AlarmTemplateService {

    private static final String ALARM_TEMPLATE_NOT_FOUND_MESSAGE = "존재하지 않는 알람 템플릿이다.";
    private static final String SENTENCE_REQUIRED_MESSAGE = "표현식이 필요하다.";
    private static final String DESCRIPTION_REQUIRED_MESSAGE = "설명이 필요하다.";

    private static final Map<String, DefaultAlarmTemplate> DEFAULT_ALARM_TEMPLATES = new LinkedHashMap<>();

    static {
        DEFAULT_ALARM_TEMPLATES.put("LIKE_MY_POST", new DefaultAlarmTemplate(
                "{handle} 님이 내 글(title)에 좋아요를 눌렀습니다.",
                "내 글에 좋아요 눌림."
        ));
        DEFAULT_ALARM_TEMPLATES.put("COMMENT_MY_POST", new DefaultAlarmTemplate(
                "{handle} 님이 내 글(comment)에 댓글을 남겼습니다.",
                "내 글에 댓글 남김."
        ));
        DEFAULT_ALARM_TEMPLATES.put("REPLY_MY_COMMENT", new DefaultAlarmTemplate(
                "{handle} 님이 내 댓글(comment)에 대댓글을 남겼습니다.",
                "내 댓글에 대댓글 남김."
        ));
        DEFAULT_ALARM_TEMPLATES.put("LIKE_MY_COMMENT", new DefaultAlarmTemplate(
                "{handle} 님이 내 댓글(comment)에 좋아요를 눌렀습니다.",
                "내 댓글에 좋아요 눌림."
        ));
    }

    private final AlarmTemplateRepository alarmTemplateRepository;

    @Transactional
    public void ensureDefaultTemplates() {
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

    public List<AlarmTemplateRes> getAdminAlarmTemplates() {
        ensureDefaultTemplates();
        return alarmTemplateRepository.findAllByOrderByAlarmTypeAsc().stream()
                .map(AlarmTemplateRes::from)
                .toList();
    }

    public AlarmTemplate getAlarmTemplate(String alarmType) {
        ensureDefaultTemplates();
        String normalizedAlarmType = normalizeAlarmType(alarmType);

        return alarmTemplateRepository.findById(normalizedAlarmType)
                .orElseThrow(() -> new BusinessException(ALARM_TEMPLATE_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

    @Transactional
    public AlarmTemplateRes updateAlarmTemplate(String alarmType, AlarmTemplateSaveReq request) {
        AlarmTemplate alarmTemplate = getAlarmTemplate(alarmType);

        alarmTemplate.changeContent(
                requireText(request.getSentence(), SENTENCE_REQUIRED_MESSAGE),
                requireText(request.getDescription(), DESCRIPTION_REQUIRED_MESSAGE)
        );
        return AlarmTemplateRes.from(alarmTemplate);
    }

    private String normalizeAlarmType(String alarmType) {
        if (alarmType == null || alarmType.isBlank()) {
            return alarmType;
        }

        return switch (alarmType) {
            case "community_post_like" -> "LIKE_MY_POST";
            case "community_post_comment" -> "COMMENT_MY_POST";
            case "community_comment_reply" -> "REPLY_MY_COMMENT";
            case "community_comment_like" -> "LIKE_MY_COMMENT";
            default -> alarmType;
        };
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }

        return value.trim();
    }

    private record DefaultAlarmTemplate(String sentence, String description) {
    }

}
