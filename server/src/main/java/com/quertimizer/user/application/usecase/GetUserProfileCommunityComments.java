package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.output.UserProfileCommunityCommentsOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileCommunityComments {

    private final UserProfileService userProfileService;

    public Optional<UserProfileCommunityCommentsOutput> execute(String targetHandle, String currentHandle) {
        // 프로필 댓글 목록을 조회
        return userProfileService.getCommunityComments(targetHandle, currentHandle);
    }
}
