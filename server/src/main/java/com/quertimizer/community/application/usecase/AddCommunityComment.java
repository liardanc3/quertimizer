package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.CommunityCommentInput;
import com.quertimizer.community.application.output.CommunityCommentOutput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AddCommunityComment {

    private final CommunityService communityService;

    public Optional<CommunityCommentOutput> execute(Long postId, String handle, CommunityCommentInput input) {
        // 게시글 댓글을 추가
        return communityService.addComment(postId, handle, input);
    }
}
