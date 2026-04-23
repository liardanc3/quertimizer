package com.quertimizer.user.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileSummaryRes {

    private final String handle;
    private final String bio;
    private final List<UserProfileLinkRes> links;
    private final String defaultDbms;
    private final boolean sqlPublic;
    private final boolean executionPercentilePublic;
    private final boolean solvedRecordsPublic;
    private final boolean solvedProblemCountPublic;
    private final Double averageExecutionPercentilePostgresql;
    private final Double averageExecutionPercentileOracle;
    private final long authoredPostCount;
    private final long likedPostCount;
    private final long commentCount;

}
