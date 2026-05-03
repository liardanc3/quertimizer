package com.quertimizer.community.application.port.out;

import com.quertimizer.community.application.output.CommunityPostPageOutput;
import com.quertimizer.community.domain.entity.CommunityPost;

import java.util.List;
import java.util.Map;

public interface CommunityPostSearchPort {

    CommunityPostPageOutput searchPosts(int requestedPage,
                                        int pageSize,
                                        String searchKeyword,
                                        String tag,
                                        String category,
                                        String sortKey,
                                        List<CommunityPost> posts,
                                        Map<Long, List<String>> tagsByPostId);

    void syncPost(CommunityPost post, List<String> tags);

    void deletePost(Long postId);
}
