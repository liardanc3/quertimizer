package com.quertimizer.community.adapter.out.persistence;

import com.quertimizer.community.domain.entity.CommunityPost;
import org.springframework.stereotype.Component;

@Component
public class CommunityPostPersistenceMapper {

    public CommunityPost toDomain(CommunityPostJpaEntity entity) {
        // 게시글 JPA 엔티티를 도메인 엔티티로 변환
        return CommunityPost.restore(
                entity.getPostId(), entity.getHandle(), entity.getTitle(),
                entity.getContentJson(), entity.getPlainTextSummary(),
                entity.getImageIds(), entity.getCategory(),
                entity.getViewCount(), entity.getLikeCount(),
                entity.getCommentCount(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    public CommunityPostJpaEntity toEntity(CommunityPost post) {
        // 게시글 도메인 엔티티를 JPA 엔티티로 변환
        return CommunityPostJpaEntity.create(
                post.getPostId(), post.getHandle(), post.getTitle(),
                post.getContentJson(), post.getPlainTextSummary(),
                post.getImageIds(), post.getCategory(),
                post.getViewCount(), post.getLikeCount(),
                post.getCommentCount(), post.getCreatedAt(), post.getUpdatedAt()
        );
    }

    public void updateEntity(CommunityPostJpaEntity entity, CommunityPost post) {
        // 게시글 도메인 상태를 기존 JPA 엔티티에 반영
        entity.update(
                post.getTitle(), post.getContentJson(),
                post.getPlainTextSummary(), post.getImageIds(),
                post.getCategory(), post.getViewCount(),
                post.getLikeCount(), post.getCommentCount(), post.getUpdatedAt()
        );
    }
}
