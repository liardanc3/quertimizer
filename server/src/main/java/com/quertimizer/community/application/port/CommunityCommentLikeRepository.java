package com.quertimizer.community.application.port;

import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.domain.entity.CommunityCommentLikeId;

import java.util.List;

public interface CommunityCommentLikeRepository {

    <S extends CommunityCommentLike> S save(S communityCommentLike);

    <S extends CommunityCommentLike> List<S> saveAll(Iterable<S> communityCommentLikes);

    boolean existsById(CommunityCommentLikeId communityCommentLikeId);

    void deleteById(CommunityCommentLikeId communityCommentLikeId);

    List<CommunityCommentLike> findAllByIdHandleOrderByCreatedAtDesc(String handle);

    void deleteAllByIdCommentIdIn(List<Long> commentIds);
}
