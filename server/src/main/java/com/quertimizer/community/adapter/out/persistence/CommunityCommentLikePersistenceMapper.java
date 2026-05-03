package com.quertimizer.community.adapter.out.persistence;

import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.domain.entity.ids.CommunityCommentLikeId;
import org.springframework.stereotype.Component;

@Component
public class CommunityCommentLikePersistenceMapper {

    public CommunityCommentLike toDomain(CommunityCommentLikeJpaEntity entity) {
        // 댓글 좋아요 JPA 엔티티를 도메인 엔티티로 변환
        return CommunityCommentLike.restore(
                entity.getId().getCommentId(),
                entity.getId().getHandle(),
                entity.getCreatedAt()
        );
    }

    public CommunityCommentLikeJpaEntity toEntity(CommunityCommentLike commentLike) {
        // 댓글 좋아요 도메인 엔티티를 JPA 엔티티로 변환
        return CommunityCommentLikeJpaEntity.create(
                commentLike.getId().getCommentId(),
                commentLike.getId().getHandle(),
                commentLike.getCreatedAt()
        );
    }

    public CommunityCommentLikeJpaId toJpaId(CommunityCommentLikeId id) {
        // 댓글 좋아요 도메인 식별자를 JPA 식별자로 변환
        return new CommunityCommentLikeJpaId(id.getCommentId(), id.getHandle());
    }
}
