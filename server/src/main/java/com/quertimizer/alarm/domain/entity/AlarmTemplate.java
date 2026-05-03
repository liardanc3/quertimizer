package com.quertimizer.alarm.domain.entity;

import lombok.Getter;

@Getter
public class AlarmTemplate {

    private String alarmType;
    private String sentence;
    private String description;

    public static AlarmTemplate create(String alarmType, String sentence, String description) {
        return new AlarmTemplate(alarmType, sentence, description);
    }

    public static AlarmTemplate restore(String alarmType, String sentence, String description) {
        // 저장된 알람 템플릿 상태 복원
        return new AlarmTemplate(alarmType, sentence, description);
    }

    public void changeContent(String sentence, String description) {
        this.sentence = sentence;
        this.description = description;
    }

    private AlarmTemplate(String alarmType, String sentence, String description) {
        this.alarmType = alarmType;
        this.sentence = sentence;
        this.description = description;
    }

}
