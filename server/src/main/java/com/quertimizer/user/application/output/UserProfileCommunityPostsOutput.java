package com.quertimizer.user.application.output;

import lombok.Data;

import java.util.List;

@Data
public class UserProfileCommunityPostsOutput {

    private final List<UserProfileCommunityPostOutput> posts;
}
