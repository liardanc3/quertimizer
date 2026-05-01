package com.quertimizer.alarm.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alarm_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlarmTemplate {

    @Id
    @Column(name = "alarm_type", nullable = false, length = 100)
    private String alarmType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sentence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "alarmTemplate")
    private List<UserAlarm> alarms = new ArrayList<>();

    public static AlarmTemplate create(String alarmType, String sentence, String description) {
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
