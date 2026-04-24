package com.quertimizer.community.application.port;

import com.quertimizer.community.domain.entity.CommunityPostTag;

import java.util.List;

public interface CommunityPostTagRepository {

    List<CommunityPostTag> findAllByPostIdOrderByTagOrderAsc(String postId);

    List<CommunityPostTag> findAllByPostIdInOrderByPostIdAscTagOrderAsc(List<String> postIds);

    void deleteAllByPostId(String postId);

    List<CommunityPostTag> findAllByTagContainingIgnoreCaseOrderByTagAsc(String tag);

    <S extends CommunityPostTag> List<S> saveAll(Iterable<S> communityPostTags);
}
