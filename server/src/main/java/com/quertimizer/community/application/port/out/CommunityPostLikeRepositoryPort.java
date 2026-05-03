package com.quertimizer.community.application.port.out;

import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.domain.entity.ids.CommunityPostLikeId;

import java.util.List;

public interface CommunityPostLikeRepositoryPort {

    CommunityPostLike save(CommunityPostLike communityPostLike);

    List<CommunityPostLike> saveAll(Iterable<CommunityPostLike> communityPostLikes);

    boolean existsById(CommunityPostLikeId communityPostLikeId);

    void deleteById(CommunityPostLikeId communityPostLikeId);

    void deleteAllByIdPostId(Long postId);

    List<CommunityPostLike> findAllByIdHandleOrderByCreatedAtDesc(String handle);

    long countByIdHandle(String handle);
}
