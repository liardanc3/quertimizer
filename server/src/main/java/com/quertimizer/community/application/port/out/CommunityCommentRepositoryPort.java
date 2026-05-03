package com.quertimizer.community.application.port.out;

import com.quertimizer.community.domain.entity.CommunityComment;

import java.util.List;
import java.util.Optional;

public interface CommunityCommentRepositoryPort {

    List<CommunityComment> findAll();

    Optional<CommunityComment> findById(Long commentId);

    CommunityComment save(CommunityComment communityComment);

    List<CommunityComment> findAllByPostIdOrderByCreatedAtAsc(Long postId);

    List<CommunityComment> findAllByHandleOrderByCreatedAtDesc(String handle);

    List<CommunityComment> findAllByCommentIdIn(List<Long> commentIds);

    long countByHandle(String handle);

    void deleteAllByPostId(Long postId);
}
