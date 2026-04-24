package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.output.CommunityPostPageOutput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetCommunityPosts {

    private final CommunityService communityService;

    public CommunityPostPageOutput execute(int page, String search, String tag, String category, String sortKey) {
        // 게시글 목록을 조회
        return communityService.getPosts(page, search, tag, category, sortKey);
    }
}
