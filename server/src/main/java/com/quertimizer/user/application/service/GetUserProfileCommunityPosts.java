package com.quertimizer.user.application.service;

import com.quertimizer.user.application.port.in.GetUserProfileCommunityPostsUseCase;
import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileCommunityPostsOutput;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileCommunityPosts implements GetUserProfileCommunityPostsUseCase {

    private final UserRepositoryPort userRepository;
    private final UserProfileService userProfileService;

    /**
     * 프로필 게시글 목록을 조회한다.
     *
     * <ol>
     *   <li>조회 대상 사용자 조회
     *   <li>공개 설정에 맞는 작성 게시글 목록 조립
     * </ol>
     *
     * @param input 조회 대상과 현재 사용자 입력
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<UserProfileCommunityPostsOutput> execute(UserProfileAccessInput input) {
        return userRepository.findByHandle(input.getTargetHandle())
                .map(user -> userProfileService.buildCommunityPosts(user, input.getCurrentHandle()));
    }
}
