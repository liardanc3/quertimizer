package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.output.UserProfileCommunityPostsOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileLikedPosts {

    private final UserProfileService userProfileService;

    public Optional<UserProfileCommunityPostsOutput> execute(String targetHandle, String currentHandle) {
        // 프로필 좋아요 게시글 목록을 조회
        return userProfileService.getLikedPosts(targetHandle, currentHandle);
    }
}
