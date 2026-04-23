package com.quertimizer.community.infrastructure.repository;

import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.domain.entity.CommunityCommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityCommentLikeRepository extends JpaRepository<CommunityCommentLike, CommunityCommentLikeId> {

    List<CommunityCommentLike> findAllByIdHandleOrderByCreatedAtDesc(String handle);

    void deleteAllByIdCommentIdIn(List<Long> commentIds);

}
