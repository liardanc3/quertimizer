package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileSubmissionSummaryOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileSubmissionSummary {

    private final UserProfileService userProfileService;

    /**
     * 프로필 제출 요약 정보를 조회한다.
     *
     * @param input 조회 대상과 현재 사용자 입력
     */
    public Optional<UserProfileSubmissionSummaryOutput> execute(UserProfileAccessInput input) {
        return userProfileService.getSubmissionSummary(input.getTargetHandle(), input.getCurrentHandle());
    }
}
