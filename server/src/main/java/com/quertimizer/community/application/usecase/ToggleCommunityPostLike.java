package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.ToggleCommunityPostLikeInput;
import com.quertimizer.community.application.output.CommunityReactionOutput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ToggleCommunityPostLike {

    private final CommunityService communityService;

    /**
     * 게시글 좋아요 상태를 토글한다.
     *
     * @param input 좋아요를 토글할 게시글과 사용자 입력
     */
    public Optional<CommunityReactionOutput> execute(ToggleCommunityPostLikeInput input) {
        return communityService.togglePostLike(input.getPostId(), input.getHandle());
    }
}
