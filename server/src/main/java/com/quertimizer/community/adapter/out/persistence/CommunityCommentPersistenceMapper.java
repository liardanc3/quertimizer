package com.quertimizer.community.adapter.out.persistence;

import com.quertimizer.community.domain.entity.CommunityComment;
import org.springframework.stereotype.Component;

@Component
public class CommunityCommentPersistenceMapper {

    public CommunityComment toDomain(CommunityCommentJpaEntity entity) {
        // 댓글 JPA 엔티티를 도메인 엔티티로 변환
        return CommunityComment.restore(
                entity.getCommentId(), entity.getPostId(),
                entity.getHandle(), entity.getParentCommentId(),
                entity.getContent(), entity.getLikeCount(), entity.getCreatedAt()
        );
    }

    public CommunityCommentJpaEntity toEntity(CommunityComment comment) {
        // 댓글 도메인 엔티티를 JPA 엔티티로 변환
        return CommunityCommentJpaEntity.create(
                comment.getPostId(), comment.getHandle(),
                comment.getParentCommentId(), comment.getContent(),
                comment.getLikeCount(), comment.getCreatedAt()
        );
    }

    public void updateEntity(CommunityCommentJpaEntity entity, CommunityComment comment) {
        // 댓글 도메인 상태를 기존 JPA 엔티티에 반영
        entity.update(comment.getContent(), comment.getLikeCount());
    }
}
