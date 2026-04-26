package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileSummaryOutput {

    private final String handle;
    private final String bio;
    private final String profileImageUrl;
    private final String backgroundImageUrl;
    private final LocalDateTime signupAt;
    private final List<UserProfileLinkOutput> links;
    private final String defaultDbms;
    private final boolean sqlPublic;
    private final boolean executionPercentilePublic;
    private final boolean solvedRecordsPublic;
    private final boolean solvedProblemCountPublic;
    private final boolean communityActivityPublic;
    private final Double averageExecutionPercentilePostgresql;
    private final Double averageExecutionPercentileOracle;
    private final long authoredPostCount;
    private final long likedPostCount;
    private final long commentCount;
}
