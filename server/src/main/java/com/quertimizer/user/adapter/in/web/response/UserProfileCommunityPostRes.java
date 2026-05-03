package com.quertimizer.user.adapter.in.web.response;

import com.quertimizer.user.application.output.UserProfileCommunityPostOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileCommunityPostRes {

    private final String postId;
    private final String title;
    private final String excerpt;
    private final List<String> tags;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int likeCount;
    private final int commentCount;

    public static UserProfileCommunityPostRes from(UserProfileCommunityPostOutput result) {
        return new UserProfileCommunityPostRes(
                result.getPostId(),
                result.getTitle(),
                result.getExcerpt(),
                result.getTags(),
                result.getCreatedAt(),
                result.getUpdatedAt(),
                result.getLikeCount(),
                result.getCommentCount()
        );
    }
}
