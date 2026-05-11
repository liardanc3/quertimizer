package com.quertimizer.user.application.output;

import lombok.Data;

import java.util.List;

@Data
public class UserProfileCommunityCommentsOutput {

    private final List<UserProfileCommunityCommentOutput> comments;
}
