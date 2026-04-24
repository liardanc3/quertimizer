package com.quertimizer.community.infrastructure.repository;

import com.quertimizer.community.application.port.CommunityPostTagRepository;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostTagJpaRepository extends JpaRepository<CommunityPostTag, Long>, CommunityPostTagRepository {
}
