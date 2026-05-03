package com.quertimizer.community.application.port.out;

import com.quertimizer.community.domain.entity.CommunityPost;

import java.util.List;
import java.util.Optional;

public interface CommunityPostRepositoryPort {

    List<CommunityPost> findAll();

    Optional<CommunityPost> findById(Long postId);

    Optional<Long> findTopPostId();

    CommunityPost save(CommunityPost communityPost);

    List<CommunityPost> saveAll(Iterable<CommunityPost> communityPosts);

    void delete(CommunityPost communityPost);

    List<CommunityPost> findAllByPostIdIn(List<Long> postIds);

    List<CommunityPost> findAllByHandleOrderByCreatedAtDesc(String handle);

    long countByHandle(String handle);
}
