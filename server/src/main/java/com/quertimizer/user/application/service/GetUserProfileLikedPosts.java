package com.quertimizer.user.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.user.application.port.in.GetUserProfileLikedPostsUseCase;
import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileCommunityPostsOutput;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileLikedPosts implements GetUserProfileLikedPostsUseCase {

    private final UserRepositoryPort userRepository;
    private final UserProfileService userProfileService;

    /**
     * 프로필 좋아요 게시글 목록을 조회한다.
     *
     * <ol>
     *   <li>조회 대상 사용자 조회
     *   <li>공개 설정에 맞는 좋아요 게시글 목록 조립
     * </ol>
     *
     * @param input 조회 대상과 현재 사용자 입력
     */
    @Transactional(readOnly = true)
    @Override
    @Log("프로필 좋아요 게시글 조회")
    public Optional<UserProfileCommunityPostsOutput> execute(UserProfileAccessInput input) {
        return userRepository.findByHandle(input.getTargetHandle())
                .map(user -> userProfileService.buildLikedPosts(user, input.getCurrentHandle()));
    }
}
