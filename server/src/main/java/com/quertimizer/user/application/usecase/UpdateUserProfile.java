package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.input.UserProfileUpdateInput;
import com.quertimizer.user.application.output.UserProfileSummaryOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateUserProfile {

    private final UserProfileService userProfileService;

    public Optional<UserProfileSummaryOutput> execute(String handle, UserProfileUpdateInput input) {
        // 프로필을 수정
        return userProfileService.updateProfile(handle, input);
    }
}
