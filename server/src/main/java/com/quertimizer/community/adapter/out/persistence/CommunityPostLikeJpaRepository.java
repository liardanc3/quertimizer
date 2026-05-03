package com.quertimizer.community.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostLikeJpaRepository extends JpaRepository<CommunityPostLikeJpaEntity, CommunityPostLikeJpaId> {
    void deleteAllByIdPostId(Long postId);
    List<CommunityPostLikeJpaEntity> findAllByIdHandleOrderByCreatedAtDesc(String handle);
    long countByIdHandle(String handle);
}
