package com.quertimizer.community.application.port.out;

import com.quertimizer.community.domain.entity.CommunityPostTag;

import java.util.List;

public interface CommunityPostTagRepositoryPort {

    List<CommunityPostTag> findAllByPostIdOrderByTagOrderAsc(Long postId);

    List<CommunityPostTag> findAllByPostIdInOrderByPostIdAscTagOrderAsc(List<Long> postIds);

    void deleteAllByPostId(Long postId);

    List<CommunityPostTag> findAllByTagContainingIgnoreCaseOrderByTagAsc(String tag);

    List<CommunityPostTag> saveAll(Iterable<CommunityPostTag> communityPostTags);
}
