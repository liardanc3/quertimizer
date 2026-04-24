package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.CommunityPostInput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateCommunityPost {

    private final CommunityService communityService;

    public String execute(String handle, CommunityPostInput input) {
        // 게시글을 생성
        return communityService.createPost(handle, input);
    }
}
