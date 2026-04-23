package com.quertimizer.user.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileCommunityCommentsRes {

    private final List<UserProfileCommunityCommentRes> comments;

}
