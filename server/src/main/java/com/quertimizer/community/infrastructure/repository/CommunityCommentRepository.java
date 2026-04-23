package com.quertimizer.community.infrastructure.repository;

import com.quertimizer.community.domain.entity.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    List<CommunityComment> findAllByPostIdOrderByCreatedAtAsc(String postId);

    List<CommunityComment> findAllByHandleOrderByCreatedAtDesc(String handle);

    List<CommunityComment> findAllByCommentIdIn(List<Long> commentIds);

    long countByHandle(String handle);

    void deleteAllByPostId(String postId);

}
