package com.quertimizer.user.adapter.out.persistence;

import com.quertimizer.user.domain.entity.UserExternalLink;
import org.springframework.stereotype.Component;

@Component
public class UserExternalLinkPersistenceMapper {

    public UserExternalLink toDomain(UserExternalLinkJpaEntity entity) {
        // 사용자 외부 링크 JPA 엔티티를 도메인 엔티티로 변환
        return UserExternalLink.restore(entity.getId().getHandle(), entity.getId().getType(), entity.getId().getLink());
    }

    public UserExternalLinkJpaEntity toEntity(UserExternalLink externalLink) {
        // 사용자 외부 링크 도메인 엔티티를 JPA 엔티티로 변환
        return UserExternalLinkJpaEntity.create(
                externalLink.getHandle(), externalLink.getType(), externalLink.getLink()
        );
    }
}
