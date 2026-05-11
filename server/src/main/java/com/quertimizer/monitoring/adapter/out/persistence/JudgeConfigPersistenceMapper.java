package com.quertimizer.monitoring.adapter.out.persistence;

import com.quertimizer.monitoring.domain.entity.JudgeConfig;
import org.springframework.stereotype.Component;

@Component
public class JudgeConfigPersistenceMapper {

    public JudgeConfigJpaEntity toEntity(JudgeConfig judgeConfig) {
        // 도메인 설정을 JPA 엔티티로 변환
        return JudgeConfigJpaEntity.create(
                judgeConfig.getDatabaseId(), judgeConfig.getDatabaseName(), judgeConfig.getDbmsType(),
                judgeConfig.isEnabled(), judgeConfig.getMaxConcurrency(), judgeConfig.getUpdatedAt()
        );
    }

    public JudgeConfig toDomain(JudgeConfigJpaEntity entity) {
        // JPA 엔티티를 도메인 설정으로 변환
        return JudgeConfig.restore(
                entity.getDatabaseId(), entity.getDatabaseName(), entity.getDbmsType(),
                entity.isEnabled(), entity.getMaxConcurrency(), entity.getUpdatedAt()
        );
    }

    public void updateEntity(JudgeConfigJpaEntity entity, JudgeConfig judgeConfig) {
        // 도메인 설정 값으로 기존 JPA 엔티티 갱신
        entity.update(
                judgeConfig.getDatabaseName(), judgeConfig.getDbmsType(),
                judgeConfig.isEnabled(), judgeConfig.getMaxConcurrency(), judgeConfig.getUpdatedAt()
        );
    }
}
