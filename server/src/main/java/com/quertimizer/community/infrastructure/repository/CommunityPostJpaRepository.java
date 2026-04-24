package com.quertimizer.community.infrastructure.repository;

import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.domain.entity.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostJpaRepository extends JpaRepository<CommunityPost, String>, CommunityPostRepository {
}
