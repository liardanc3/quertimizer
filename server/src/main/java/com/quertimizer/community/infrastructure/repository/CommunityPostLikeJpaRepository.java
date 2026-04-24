package com.quertimizer.community.infrastructure.repository;

import com.quertimizer.community.application.port.CommunityPostLikeRepository;
import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.domain.entity.CommunityPostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostLikeJpaRepository extends JpaRepository<CommunityPostLike, CommunityPostLikeId>, CommunityPostLikeRepository {
}
