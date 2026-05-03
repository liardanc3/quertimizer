package com.quertimizer.community.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCommentLikeJpaRepository
        extends JpaRepository<CommunityCommentLikeJpaEntity, CommunityCommentLikeJpaId> {
    List<CommunityCommentLikeJpaEntity> findAllByIdHandleOrderByCreatedAtDesc(String handle);
    void deleteAllByIdCommentIdIn(List<Long> commentIds);
}
