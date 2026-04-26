package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.output.UserProfileSubmissionSummaryOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileSubmissionSummary {

    private final UserProfileService userProfileService;

    public Optional<UserProfileSubmissionSummaryOutput> execute(String targetHandle, String currentHandle) {
        // 프로필 제출 요약 정보를 조회
        return userProfileService.getSubmissionSummary(targetHandle, currentHandle);
    }
}
