package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.ToggleCommunityCommentLikeInput;
import com.quertimizer.community.application.output.CommunityReactionOutput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ToggleCommunityCommentLike {

    private final CommunityService communityService;

    /**
     * 댓글 좋아요 상태를 토글한다.
     *
     * @param input 좋아요를 토글할 댓글과 사용자 입력
     */
    public Optional<CommunityReactionOutput> execute(ToggleCommunityCommentLikeInput input) {
        return communityService.toggleCommentLike(input.getCommentId(), input.getHandle());
    }
}
