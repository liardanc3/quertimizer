package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileCommunityCommentsOutput;
import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileCommunityComments {

    private final UserRepository userRepository;
    private final UserProfileService userProfileService;

    /**
     * 프로필 댓글 목록을 조회한다.
     *
     * <ol>
     *   <li>조회 대상 사용자 조회
     *   <li>공개 설정에 맞는 작성 댓글 목록 조립
     * </ol>
     *
     * @param input 조회 대상과 현재 사용자 입력
     */
    @Transactional(readOnly = true)
    public Optional<UserProfileCommunityCommentsOutput> execute(UserProfileAccessInput input) {
        return userRepository.findByHandle(input.getTargetHandle())
                .map(user -> userProfileService.buildCommunityComments(user, input.getCurrentHandle()));
    }
}
