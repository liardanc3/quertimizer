package com.quertimizer.community.application.port;

import com.quertimizer.community.domain.entity.CommunityPost;

import java.util.List;
import java.util.Optional;

public interface CommunityPostRepository {

    List<CommunityPost> findAll();

    Optional<CommunityPost> findById(String postId);

    <S extends CommunityPost> S save(S communityPost);

    <S extends CommunityPost> List<S> saveAll(Iterable<S> communityPosts);

    void delete(CommunityPost communityPost);

    List<CommunityPost> findAllByPostIdIn(List<String> postIds);

    List<CommunityPost> findAllByHandleOrderByCreatedAtDesc(String handle);

    long countByHandle(String handle);
}
