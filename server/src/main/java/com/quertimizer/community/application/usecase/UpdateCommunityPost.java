package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.UpdateCommunityPostInput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateCommunityPost {

    private final CommunityService communityService;

    /**
     * 커뮤니티 게시글을 수정한다.
     *
     * @param input 수정할 게시글, 요청자, 저장할 게시글 입력
     */
    public Optional<Long> execute(UpdateCommunityPostInput input) {
        return communityService.updatePost(input.getPostId(), input.getHandle(), input.getPost());
    }
}
