package com.quertimizer.user.adapter.in.http.response;

import com.quertimizer.user.application.output.UserProfileCommunityPostsOutput;
import lombok.Data;

import java.util.List;

@Data
public class UserProfileCommunityPostsRes {

    private final List<UserProfileCommunityPostRes> posts;

    public static UserProfileCommunityPostsRes from(UserProfileCommunityPostsOutput result) {
        return new UserProfileCommunityPostsRes(result.getPosts().stream()
                .map(UserProfileCommunityPostRes::from)
                .toList());
    }
}
