package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.output.UserProfileSummaryOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileSummary {

    private final UserProfileService userProfileService;

    public Optional<UserProfileSummaryOutput> execute(String targetHandle, String currentHandle) {
        // 프로필 요약 정보를 조회
        return userProfileService.getProfileSummary(targetHandle, currentHandle);
    }
}
