package com.quertimizer.user.presentation.dto.response;

import com.quertimizer.user.application.output.UserProfileCommunityCommentOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserProfileCommunityCommentRes {

    private final Long commentId;
    private final String postId;
    private final String postTitle;
    private final String content;
    private final LocalDateTime createdAt;
    private final boolean reply;

    public static UserProfileCommunityCommentRes from(UserProfileCommunityCommentOutput result) {
        return new UserProfileCommunityCommentRes(
                result.getCommentId(),
                result.getPostId(),
                result.getPostTitle(),
                result.getContent(),
                result.getCreatedAt(),
                result.isReply()
        );
    }
}
