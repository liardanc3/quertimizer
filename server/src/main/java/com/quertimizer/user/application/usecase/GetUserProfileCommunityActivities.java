package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.input.UserProfileActivityPageInput;
import com.quertimizer.user.application.output.UserProfileCommunityActivitiesOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileCommunityActivities {

    private final UserProfileService userProfileService;

    /**
     * 프로필 커뮤니티 활동 페이지를 조회한다.
     *
     * @param input 조회 대상, 현재 사용자, 페이지 입력
     */
    public Optional<UserProfileCommunityActivitiesOutput> execute(UserProfileActivityPageInput input) {
        return userProfileService.getCommunityActivities(
                input.getTargetHandle(), input.getCurrentHandle(), input.getPage(), input.getPageSize()
        );
    }
}
