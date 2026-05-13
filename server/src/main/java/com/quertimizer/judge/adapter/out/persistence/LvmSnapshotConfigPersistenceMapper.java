package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.domain.entity.LvmSnapshotConfig;
import org.springframework.stereotype.Component;

@Component
public class LvmSnapshotConfigPersistenceMapper {

    public LvmSnapshotConfig toDomain(LvmSnapshotConfigJpaEntity entity) {
        // JPA 엔티티를 DB 실행 환경 도메인 설정으로 변환
        return LvmSnapshotConfig.restore(
                entity.getConfigId(), entity.getMountRoot(), entity.getVolumeGroup(), entity.getThinPool(),
                entity.getBaseTemplateVersion(), entity.getCommandTimeoutSeconds(),
                entity.getStartupTimeoutSeconds(), entity.getUpdatedAt()
        );
    }
}
