package com.quertimizer.community.adapter.out.persistence;

import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.domain.entity.ids.CommunityPostLikeId;
import org.springframework.stereotype.Component;

@Component
public class CommunityPostLikePersistenceMapper {

    public CommunityPostLike toDomain(CommunityPostLikeJpaEntity entity) {
        // 게시글 좋아요 JPA 엔티티를 도메인 엔티티로 변환
        return CommunityPostLike.restore(entity.getId().getPostId(), entity.getId().getHandle(), entity.getCreatedAt());
    }

    public CommunityPostLikeJpaEntity toEntity(CommunityPostLike postLike) {
        // 게시글 좋아요 도메인 엔티티를 JPA 엔티티로 변환
        return CommunityPostLikeJpaEntity.create(
                postLike.getId().getPostId(),
                postLike.getId().getHandle(),
                postLike.getCreatedAt()
        );
    }

    public CommunityPostLikeJpaId toJpaId(CommunityPostLikeId id) {
        // 게시글 좋아요 도메인 식별자를 JPA 식별자로 변환
        return new CommunityPostLikeJpaId(id.getPostId(), id.getHandle());
    }
}
