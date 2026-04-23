package com.quertimizer.community.infrastructure.repository;

import com.quertimizer.community.domain.entity.CommunityPostTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostTagRepository extends JpaRepository<CommunityPostTag, Long> {

    List<CommunityPostTag> findAllByPostIdOrderByTagOrderAsc(String postId);

    List<CommunityPostTag> findAllByPostIdInOrderByPostIdAscTagOrderAsc(List<String> postIds);

    void deleteAllByPostId(String postId);

    List<CommunityPostTag> findAllByTagContainingIgnoreCaseOrderByTagAsc(String tag);

}
