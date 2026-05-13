package com.quertimizer.user.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.user.application.port.in.UpdateUserProfileUseCase;
import com.quertimizer.user.application.input.UserProfileUpdateCommandInput;
import com.quertimizer.user.application.output.UserProfileSummaryOutput;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateUserProfile implements UpdateUserProfileUseCase {

    private final UserRepositoryPort userRepository;
    private final UserProfileService userProfileService;

    /**
     * 프로필을 수정한다.
     *
     * <ol>
     *   <li>수정 대상 사용자 조회
     *   <li>프로필 정보와 외부 링크 변경
     * </ol>
     *
     * @param input 수정 대상과 저장할 프로필 입력
     */
    @Transactional
    @Override
    @Log("프로필 수정")
    public Optional<UserProfileSummaryOutput> execute(UserProfileUpdateCommandInput input) {
        return userRepository.findByHandle(input.getHandle())
                .map(user -> userProfileService.updateProfile(user, input.getProfile()));
    }
}
