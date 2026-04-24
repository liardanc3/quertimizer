package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.output.CommunityReactionOutput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ToggleCommunityPostLike {

    private final CommunityService communityService;

    public Optional<CommunityReactionOutput> execute(String postId, String handle) {
        // 게시글 좋아요를 토글
        return communityService.togglePostLike(postId, handle);
    }
}
