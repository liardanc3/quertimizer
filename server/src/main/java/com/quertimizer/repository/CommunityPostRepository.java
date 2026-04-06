package com.quertimizer.repository;

import com.quertimizer.entity.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, String> {

    List<CommunityPost> findAllByPostIdIn(List<String> postIds);

    List<CommunityPost> findAllByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserId(String userId);

}
