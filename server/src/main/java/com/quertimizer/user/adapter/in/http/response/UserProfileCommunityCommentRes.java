package com.quertimizer.user.adapter.in.http.response;

import com.quertimizer.user.application.output.UserProfileCommunityCommentOutput;
import lombok.Data;

import java.time.LocalDateTime;

@Data
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
