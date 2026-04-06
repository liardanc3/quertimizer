package com.quertimizer.repository;

import com.quertimizer.entity.CommunityPostLike;
import com.quertimizer.entity.CommunityPostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, CommunityPostLikeId> {

    void deleteAllByIdPostId(String postId);

    List<CommunityPostLike> findAllByIdUserIdOrderByCreatedAtDesc(String userId);

    long countByIdUserId(String userId);

}
