package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.DeleteCommunityPostInput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteCommunityPost {

    private final CommunityService communityService;

    /**
     * 커뮤니티 게시글을 삭제한다.
     *
     * @param input 삭제할 게시글과 요청자 입력
     */
    public boolean execute(DeleteCommunityPostInput input) {
        return communityService.deletePost(input.getPostId(), input.getHandle());
    }
}
