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

    public List<CommunityTagSuggestionOutput> execute(String query) {
        // 게시글 태그 자동완성 후보를 조회
        return communityService.getTagSuggestions(query);
    }
}
