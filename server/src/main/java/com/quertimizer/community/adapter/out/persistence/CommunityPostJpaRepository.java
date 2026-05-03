package com.quertimizer.community.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityPostJpaRepository extends JpaRepository<CommunityPostJpaEntity, Long> {
    @Query("select max(post.postId) from CommunityPostJpaEntity post")
    Optional<Long> findTopPostId();
    List<CommunityPostJpaEntity> findAllByPostIdIn(List<Long> postIds);
    List<CommunityPostJpaEntity> findAllByHandleOrderByCreatedAtDesc(String handle);
    long countByHandle(String handle);
}
