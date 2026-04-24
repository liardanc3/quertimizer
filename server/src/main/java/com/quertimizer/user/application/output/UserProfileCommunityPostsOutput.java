package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileCommunityPostsOutput {

    private final List<UserProfileCommunityPostOutput> posts;
}
