package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.output.CommunityTagSuggestionOutput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetCommunityTagSuggestions {

    private final CommunityService communityService;

    /**
     * 게시글 태그 자동완성 후보를 조회한다.
     *
     * @param query 태그 검색어
     */
    public List<CommunityTagSuggestionOutput> execute(String query) {
        return communityService.getTagSuggestions(query);
    }
}
