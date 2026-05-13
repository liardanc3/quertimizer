package com.quertimizer.user.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.user.application.port.in.GetUserProfileSummaryUseCase;
import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileSummaryOutput;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileSummary implements GetUserProfileSummaryUseCase {

    private final UserRepositoryPort userRepository;
    private final UserProfileService userProfileService;

    /**
     * 프로필 요약 정보를 조회한다.
     *
     * <ol>
     *   <li>조회 대상 사용자 조회
     *   <li>본인 여부에 맞는 프로필 요약 조립
     * </ol>
     *
     * @param input 조회 대상과 현재 사용자 입력
     */
    @Transactional
    @Override
    @Log("프로필 요약 조회")
    public Optional<UserProfileSummaryOutput> execute(UserProfileAccessInput input) {
        boolean isOwnProfile = input.getTargetHandle().equals(input.getCurrentHandle());
        return userRepository.findByHandle(input.getTargetHandle())
                .map(user -> userProfileService.buildUserProfileSummary(user, isOwnProfile));
    }
}
