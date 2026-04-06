package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileCommunityPostsRes {

    private final List<UserProfileCommunityPostRes> posts;

}
