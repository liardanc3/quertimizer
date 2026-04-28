package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.input.UserProfileUpdateCommandInput;
import com.quertimizer.user.application.output.UserProfileSummaryOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateUserProfile {

    private final UserProfileService userProfileService;

    /**
     * 프로필을 수정한다.
     *
     * @param input 수정 대상과 저장할 프로필 입력
     */
    public Optional<UserProfileSummaryOutput> execute(UserProfileUpdateCommandInput input) {
        return userProfileService.updateProfile(input.getHandle(), input.getProfile());
    }
}
