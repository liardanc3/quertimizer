package com.quertimizer.user.application.service;

import com.quertimizer.user.application.port.in.GetUserProfileSubmissionSummaryUseCase;
import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileSubmissionSummaryOutput;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileSubmissionSummary implements GetUserProfileSubmissionSummaryUseCase {

    private final UserRepositoryPort userRepository;
    private final UserProfileService userProfileService;

    /**
     * 프로필 제출 요약 정보를 조회한다.
     *
     * <ol>
     *   <li>조회 대상 사용자 조회
     *   <li>제출 요약 조립
     * </ol>
     *
     * @param input 조회 대상과 현재 사용자 입력
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<UserProfileSubmissionSummaryOutput> execute(UserProfileAccessInput input) {
        boolean isOwnProfile = input.getTargetHandle().equals(input.getCurrentHandle());
        return userRepository.findByHandle(input.getTargetHandle())
                .map(user -> userProfileService.buildSubmissionSummary(user, isOwnProfile));
    }
}
