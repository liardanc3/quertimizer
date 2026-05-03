package com.quertimizer.user.adapter.in.web.response;

import com.quertimizer.user.application.output.UserProfileCommunityCommentsOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileCommunityCommentsRes {

    private final List<UserProfileCommunityCommentRes> comments;

    public static UserProfileCommunityCommentsRes from(UserProfileCommunityCommentsOutput result) {
        return new UserProfileCommunityCommentsRes(result.getComments().stream()
                .map(UserProfileCommunityCommentRes::from)
                .toList());
    }
}
