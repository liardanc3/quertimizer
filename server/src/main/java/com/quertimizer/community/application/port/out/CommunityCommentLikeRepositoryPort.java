package com.quertimizer.community.application.port.out;

import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.domain.entity.ids.CommunityCommentLikeId;

import java.util.List;

public interface CommunityCommentLikeRepositoryPort {

    CommunityCommentLike save(CommunityCommentLike communityCommentLike);

    List<CommunityCommentLike> saveAll(Iterable<CommunityCommentLike> communityCommentLikes);

    boolean existsById(CommunityCommentLikeId communityCommentLikeId);

    void deleteById(CommunityCommentLikeId communityCommentLikeId);

    List<CommunityCommentLike> findAllByIdHandleOrderByCreatedAtDesc(String handle);

    void deleteAllByIdCommentIdIn(List<Long> commentIds);
}
