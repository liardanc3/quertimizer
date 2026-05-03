package com.quertimizer.community.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCommentJpaRepository extends JpaRepository<CommunityCommentJpaEntity, Long> {
    List<CommunityCommentJpaEntity> findAllByPostIdOrderByCreatedAtAsc(Long postId);
    List<CommunityCommentJpaEntity> findAllByHandleOrderByCreatedAtDesc(String handle);
    List<CommunityCommentJpaEntity> findAllByCommentIdIn(List<Long> commentIds);
    long countByHandle(String handle);
    void deleteAllByPostId(Long postId);
}
