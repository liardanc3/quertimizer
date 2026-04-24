package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.output.CommunityReactionOutput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ToggleCommunityCommentLike {

    private final CommunityService communityService;

    public Optional<CommunityReactionOutput> execute(Long commentId, String handle) {
        // 댓글 좋아요를 토글
        return communityService.toggleCommentLike(commentId, handle);
    }
}
