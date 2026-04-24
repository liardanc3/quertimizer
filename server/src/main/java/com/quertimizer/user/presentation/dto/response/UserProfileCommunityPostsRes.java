package com.quertimizer.user.presentation.dto.response;

import com.quertimizer.user.application.output.UserProfileCommunityPostsOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileCommunityPostsRes {

    private final List<UserProfileCommunityPostRes> posts;

    public static UserProfileCommunityPostsRes from(UserProfileCommunityPostsOutput result) {
        return new UserProfileCommunityPostsRes(result.getPosts().stream()
                .map(UserProfileCommunityPostRes::from)
                .toList());
    }
}
