package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteCommunityPost {

    private final CommunityService communityService;

    public boolean execute(Long postId, String handle) {
        // 게시글을 삭제
        return communityService.deletePost(postId, handle);
    }
}
