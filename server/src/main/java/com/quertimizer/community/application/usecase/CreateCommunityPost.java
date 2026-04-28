package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.CreateCommunityPostInput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateCommunityPost {

    private final CommunityService communityService;

    /**
     * 커뮤니티 게시글을 생성한다.
     *
     * @param input 게시글 작성자와 저장할 게시글 입력
     */
    public Long execute(CreateCommunityPostInput input) {
        return communityService.createPost(input.getHandle(), input.getPost());
    }
}
