package com.quertimizer.alarm.adapter.out.persistence;

import com.quertimizer.alarm.domain.entity.UserAlarm;
import org.springframework.stereotype.Component;

@Component
public class UserAlarmPersistenceMapper {

    public UserAlarm toDomain(UserAlarmJpaEntity entity) {
        // 사용자 알람 JPA 엔티티를 도메인 엔티티로 변환
        return UserAlarm.restore(
                entity.getAlarmId(), entity.getHandle(),
                entity.getAlarmType(), entity.getTitle(),
                entity.getMessage(), entity.getTargetPath(),
                entity.getTargetHash(), entity.getBindingsJson(),
                entity.isRead(), entity.getCreatedAt()
        );
    }

    public UserAlarmJpaEntity toEntity(UserAlarm userAlarm) {
        // 사용자 알람 도메인 엔티티를 JPA 엔티티로 변환
        return UserAlarmJpaEntity.create(
                userAlarm.getHandle(), userAlarm.getAlarmType(),
                userAlarm.getTitle(), userAlarm.getMessage(),
                userAlarm.getTargetPath(), userAlarm.getTargetHash(),
                userAlarm.getBindingsJson(), userAlarm.isRead(),
                userAlarm.getCreatedAt()
        );
    }

    public void updateEntity(UserAlarmJpaEntity entity, UserAlarm userAlarm) {
        // 사용자 알람 도메인 상태를 기존 JPA 엔티티에 반영
        entity.update(userAlarm.isRead());
    }
}
