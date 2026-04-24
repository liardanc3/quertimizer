package com.quertimizer.community.infrastructure.repository;

import com.quertimizer.community.application.port.CommunityCommentRepository;
import com.quertimizer.community.domain.entity.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCommentJpaRepository extends JpaRepository<CommunityComment, Long>, CommunityCommentRepository {
}
