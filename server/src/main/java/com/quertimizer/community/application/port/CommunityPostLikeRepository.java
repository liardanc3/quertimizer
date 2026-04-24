package com.quertimizer.community.application.port;

import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.domain.entity.CommunityPostLikeId;

import java.util.List;

public interface CommunityPostLikeRepository {

    <S extends CommunityPostLike> S save(S communityPostLike);

    <S extends CommunityPostLike> List<S> saveAll(Iterable<S> communityPostLikes);

    boolean existsById(CommunityPostLikeId communityPostLikeId);

    void deleteById(CommunityPostLikeId communityPostLikeId);

    void deleteAllByIdPostId(String postId);

    List<CommunityPostLike> findAllByIdHandleOrderByCreatedAtDesc(String handle);

    long countByIdHandle(String handle);
}
