package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.output.UserProfileCommunityActivitiesOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileCommunityActivities {

    private final UserProfileService userProfileService;

    public Optional<UserProfileCommunityActivitiesOutput> execute(String targetHandle, String currentHandle, int page, Integer pageSize) {
        // 프로필 커뮤니티 활동 페이지를 조회
        return userProfileService.getCommunityActivities(targetHandle, currentHandle, page, pageSize);
    }
}
