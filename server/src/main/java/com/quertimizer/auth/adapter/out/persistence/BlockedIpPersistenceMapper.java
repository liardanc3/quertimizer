package com.quertimizer.auth.adapter.out.persistence;

import com.quertimizer.auth.domain.entity.BlockedIp;
import org.springframework.stereotype.Component;

@Component
public class BlockedIpPersistenceMapper {

    public BlockedIp toDomain(BlockedIpJpaEntity entity) {
        // 차단 IP JPA 엔티티를 도메인 엔티티로 변환
        return BlockedIp.restore(entity.getIpAddress(), entity.getBlockedHandle(), entity.getBlockedAt());
    }

    public BlockedIpJpaEntity toEntity(BlockedIp blockedIp) {
        // 차단 IP 도메인 엔티티를 JPA 엔티티로 변환
        return BlockedIpJpaEntity.create(
                blockedIp.getIpAddress(),
                blockedIp.getBlockedHandle(),
                blockedIp.getBlockedAt()
        );
    }

    public void updateEntity(BlockedIpJpaEntity entity, BlockedIp blockedIp) {
        // 차단 IP 도메인 상태를 기존 JPA 엔티티에 반영
        entity.update(blockedIp.getBlockedHandle(), blockedIp.getBlockedAt());
    }
}
