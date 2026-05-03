package com.quertimizer.community.adapter.out.persistence;

import com.quertimizer.community.domain.entity.CommunityPostTag;
import org.springframework.stereotype.Component;

@Component
public class CommunityPostTagPersistenceMapper {

    public CommunityPostTag toDomain(CommunityPostTagJpaEntity entity) {
        // 게시글 태그 JPA 엔티티를 도메인 엔티티로 변환
        return CommunityPostTag.restore(entity.getTagId(), entity.getPostId(), entity.getTag(), entity.getTagOrder());
    }

    public CommunityPostTagJpaEntity toEntity(CommunityPostTag tag) {
        // 게시글 태그 도메인 엔티티를 JPA 엔티티로 변환
        return CommunityPostTagJpaEntity.create(tag.getPostId(), tag.getTag(), tag.getTagOrder());
    }
}
