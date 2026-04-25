package com.quertimizer.community.infrastructure.repository;

import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.domain.entity.CommunityPost;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityPostJpaRepository extends JpaRepository<CommunityPost, Long>, CommunityPostRepository {

    @Override
    @Query("select max(post.postId) from CommunityPost post")
    Optional<Long> findTopPostId();
}
