package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileCommunityCommentsOutput {

    private final List<UserProfileCommunityCommentOutput> comments;
}
