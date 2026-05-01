package com.quertimizer.alarm.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AlarmTemplateDefaults {

    public static final Map<String, AlarmTemplateDefault> VALUES = createValues();

    private AlarmTemplateDefaults() {
    }

    private static Map<String, AlarmTemplateDefault> createValues() {
        // 기본 알람 템플릿 목록 생성
        Map<String, AlarmTemplateDefault> values = new LinkedHashMap<>();
        values.put(AlarmType.LIKE_MY_POST.getValue(), new AlarmTemplateDefault(
                AlarmType.LIKE_MY_POST.getDefaultSentence(),
                AlarmType.LIKE_MY_POST.getDefaultDescription()
        ));
        values.put(AlarmType.COMMENT_MY_POST.getValue(), new AlarmTemplateDefault(
                AlarmType.COMMENT_MY_POST.getDefaultSentence(),
                AlarmType.COMMENT_MY_POST.getDefaultDescription()
        ));
        values.put(AlarmType.REPLY_MY_COMMENT.getValue(), new AlarmTemplateDefault(
                AlarmType.REPLY_MY_COMMENT.getDefaultSentence(),
                AlarmType.REPLY_MY_COMMENT.getDefaultDescription()
        ));
        values.put(AlarmType.LIKE_MY_COMMENT.getValue(), new AlarmTemplateDefault(
                AlarmType.LIKE_MY_COMMENT.getDefaultSentence(),
                AlarmType.LIKE_MY_COMMENT.getDefaultDescription()
        ));
        return Collections.unmodifiableMap(values);
    }
}
