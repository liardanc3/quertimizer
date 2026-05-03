package com.quertimizer.community.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostTagJpaRepository extends JpaRepository<CommunityPostTagJpaEntity, Long> {
    List<CommunityPostTagJpaEntity> findAllByPostIdOrderByTagOrderAsc(Long postId);
    List<CommunityPostTagJpaEntity> findAllByPostIdInOrderByPostIdAscTagOrderAsc(List<Long> postIds);
    void deleteAllByPostId(Long postId);
    List<CommunityPostTagJpaEntity> findAllByTagContainingIgnoreCaseOrderByTagAsc(String tag);
}
