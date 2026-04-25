package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.CommunityPostInput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateCommunityPost {

    private final CommunityService communityService;

    public Optional<Long> execute(Long postId, String handle, CommunityPostInput input) {
        // 게시글을 수정
        return communityService.updatePost(postId, handle, input);
    }
}
