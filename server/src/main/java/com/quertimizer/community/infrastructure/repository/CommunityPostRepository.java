package com.quertimizer.community.infrastructure.repository;

import com.quertimizer.community.domain.entity.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, String> {

    List<CommunityPost> findAllByPostIdIn(List<String> postIds);

    List<CommunityPost> findAllByHandleOrderByCreatedAtDesc(String handle);

    long countByHandle(String handle);

}
