package com.quertimizer.community.infrastructure.repository;

import com.quertimizer.community.application.port.CommunityCommentLikeRepository;
import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.domain.entity.ids.CommunityCommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCommentLikeJpaRepository
        extends JpaRepository<CommunityCommentLike, CommunityCommentLikeId>, CommunityCommentLikeRepository {
}
