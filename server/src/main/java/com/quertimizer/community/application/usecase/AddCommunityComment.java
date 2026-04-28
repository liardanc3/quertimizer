package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.AddCommunityCommentInput;
import com.quertimizer.community.application.output.CommunityCommentOutput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AddCommunityComment {

    private final CommunityService communityService;

    /**
     * 게시글 댓글을 추가한다.
     *
     * @param input 댓글을 추가할 게시글, 작성자, 댓글 내용 입력
     */
    public Optional<CommunityCommentOutput> execute(AddCommunityCommentInput input) {
        return communityService.addComment(input.getPostId(), input.getHandle(), input.getComment());
    }
}
