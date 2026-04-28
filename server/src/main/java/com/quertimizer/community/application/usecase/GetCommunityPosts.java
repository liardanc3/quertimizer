package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.CommunityPostSearchInput;
import com.quertimizer.community.application.output.CommunityPostPageOutput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetCommunityPosts {

    private final CommunityService communityService;

    /**
     * 커뮤니티 게시글 목록을 검색 조건에 맞게 조회한다.
     *
     * @param input 게시글 검색, 필터, 정렬 입력
     */
    public CommunityPostPageOutput execute(CommunityPostSearchInput input) {
        return communityService.getPosts(
                input.getPage(), input.getSearch(), input.getTag(), input.getCategory(), input.getSortKey()
        );
    }
}
