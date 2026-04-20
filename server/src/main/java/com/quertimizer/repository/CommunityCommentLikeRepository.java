package com.quertimizer.repository;

import com.quertimizer.entity.CommunityCommentLike;
import com.quertimizer.entity.CommunityCommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityCommentLikeRepository extends JpaRepository<CommunityCommentLike, CommunityCommentLikeId> {

    List<CommunityCommentLike> findAllByIdUserIdOrderByCreatedAtDesc(String userId);

    void deleteAllByIdCommentIdIn(List<Long> commentIds);

}
