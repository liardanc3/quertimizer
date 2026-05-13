package com.quertimizer.user.adapter.in.http.response;

import com.quertimizer.user.application.output.UserProfileCommunityCommentsOutput;
import lombok.Data;

import java.util.List;

@Data
public class UserProfileCommunityCommentsRes {

    private final List<UserProfileCommunityCommentRes> comments;

    public static UserProfileCommunityCommentsRes from(UserProfileCommunityCommentsOutput result) {
        return new UserProfileCommunityCommentsRes(result.getComments().stream()
                .map(UserProfileCommunityCommentRes::from)
                .toList());
    }
}
