package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileSolvedProblemsOutput;
import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileSolvedProblems {

    private final UserRepository userRepository;
    private final UserProfileService userProfileService;

    /**
     * 프로필 해결한 문제 목록을 조회한다.
     *
     * <ol>
     *   <li>조회 대상 사용자 조회
     *   <li>공개 설정에 맞는 해결 문제 목록 조립
     * </ol>
     *
     * @param input 조회 대상과 현재 사용자 입력
     */
    @Transactional(readOnly = true)
    public Optional<UserProfileSolvedProblemsOutput> execute(UserProfileAccessInput input) {
        boolean isOwnProfile = input.getTargetHandle().equals(input.getCurrentHandle());
        return userRepository.findByHandle(input.getTargetHandle())
                .map(user -> userProfileService.buildSolvedProblems(user, isOwnProfile));
    }
}
