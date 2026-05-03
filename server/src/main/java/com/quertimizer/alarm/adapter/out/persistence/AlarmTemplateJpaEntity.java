package com.quertimizer.alarm.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alarm_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlarmTemplateJpaEntity {

    @Id
    @Column(name = "alarm_type", nullable = false, length = 100)
    private String alarmType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sentence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    public static AlarmTemplateJpaEntity create(String alarmType, String sentence, String description) {
        // 알람 템플릿 JPA 엔티티 생성
        return new AlarmTemplateJpaEntity(alarmType, sentence, description);
    }

    public void update(String sentence, String description) {
        // 알람 템플릿 내용 변경
        this.sentence = sentence;
        this.description = description;
    }

    private AlarmTemplateJpaEntity(String alarmType, String sentence, String description) {
        this.alarmType = alarmType;
        this.sentence = sentence;
        this.description = description;
    }
}
