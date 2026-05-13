package com.quertimizer.user.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.user.application.port.in.GetUserProfileCommunityActivitiesUseCase;
import com.quertimizer.user.application.input.UserProfileActivityPageInput;
import com.quertimizer.user.application.output.UserProfileCommunityActivitiesOutput;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileCommunityActivities implements GetUserProfileCommunityActivitiesUseCase {

    private final UserRepositoryPort userRepository;
    private final UserProfileService userProfileService;

    /**
     * 프로필 커뮤니티 활동 페이지를 조회한다.
     *
     * <ol>
     *   <li>조회 대상 사용자 조회
     *   <li>공개 설정에 맞는 커뮤니티 활동 페이지 조립
     * </ol>
     *
     * @param input 조회 대상, 현재 사용자, 페이지 입력
     */
    @Transactional(readOnly = true)
    @Override
    @Log("프로필 커뮤니티 활동 조회")
    public Optional<UserProfileCommunityActivitiesOutput> execute(UserProfileActivityPageInput input) {
        return userRepository.findByHandle(input.getTargetHandle())
                .map(user -> userProfileService.buildCommunityActivities(
                        user, input.getCurrentHandle(), input.getPage(), input.getPageSize()
                ));
    }
}
