package com.quertimizer.repository;

import com.quertimizer.entity.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    List<CommunityComment> findAllByPostIdOrderByCreatedAtAsc(String postId);

    List<CommunityComment> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<CommunityComment> findAllByCommentIdIn(List<Long> commentIds);

    long countByUserId(String userId);

    void deleteAllByPostId(String postId);

}
