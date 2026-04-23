package com.quertimizer.community.infrastructure.repository;

import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.domain.entity.CommunityPostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, CommunityPostLikeId> {

    void deleteAllByIdPostId(String postId);

    List<CommunityPostLike> findAllByIdHandleOrderByCreatedAtDesc(String handle);

    long countByIdHandle(String handle);

}
