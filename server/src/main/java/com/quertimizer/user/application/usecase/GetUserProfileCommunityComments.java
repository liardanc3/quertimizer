package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileCommunityCommentsOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileCommunityComments {

    private final UserProfileService userProfileService;

    /**
     * 프로필 댓글 목록을 조회한다.
     *
     * @param input 조회 대상과 현재 사용자 입력
     */
    public Optional<UserProfileCommunityCommentsOutput> execute(UserProfileAccessInput input) {
        return userProfileService.getCommunityComments(input.getTargetHandle(), input.getCurrentHandle());
    }
}
