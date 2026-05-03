package com.quertimizer.alarm.adapter.out.persistence;

import com.quertimizer.alarm.domain.entity.AlarmTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlarmTemplatePersistenceMapper {

    public AlarmTemplate toDomain(AlarmTemplateJpaEntity entity) {
        // 알람 템플릿 JPA 엔티티를 도메인 엔티티로 변환
        return AlarmTemplate.restore(entity.getAlarmType(), entity.getSentence(), entity.getDescription());
    }

    public AlarmTemplateJpaEntity toEntity(AlarmTemplate alarmTemplate) {
        // 알람 템플릿 도메인 엔티티를 JPA 엔티티로 변환
        return AlarmTemplateJpaEntity.create(
                alarmTemplate.getAlarmType(),
                alarmTemplate.getSentence(),
                alarmTemplate.getDescription()
        );
    }

    public void updateEntity(AlarmTemplateJpaEntity entity, AlarmTemplate alarmTemplate) {
        // 알람 템플릿 도메인 상태를 기존 JPA 엔티티에 반영
        entity.update(alarmTemplate.getSentence(), alarmTemplate.getDescription());
    }
}
