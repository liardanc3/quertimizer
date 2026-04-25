package com.quertimizer.community.application.port;

import com.quertimizer.community.domain.entity.CommunityPostTag;

import java.util.List;

public interface CommunityPostTagRepository {

    List<CommunityPostTag> findAllByPostIdOrderByTagOrderAsc(Long postId);

    List<CommunityPostTag> findAllByPostIdInOrderByPostIdAscTagOrderAsc(List<Long> postIds);

    void deleteAllByPostId(Long postId);

    List<CommunityPostTag> findAllByTagContainingIgnoreCaseOrderByTagAsc(String tag);

    <S extends CommunityPostTag> List<S> saveAll(Iterable<S> communityPostTags);
}
