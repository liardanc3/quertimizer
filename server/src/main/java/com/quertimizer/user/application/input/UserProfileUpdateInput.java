package com.quertimizer.user.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

import java.util.List;

@Data
public class UserProfileUpdateInput {

    private final String bio;
    private final String profileImageUrl;
    private final String backgroundImageUrl;
    private final List<UserProfileLinkInput> links;
    private final DbmsType defaultDbms;
    private final boolean sqlPublic;
    private final boolean executionPercentilePublic;
    private final boolean solvedRecordsPublic;
    private final boolean solvedProblemCountPublic;
    private final boolean communityActivityPublic;
}
