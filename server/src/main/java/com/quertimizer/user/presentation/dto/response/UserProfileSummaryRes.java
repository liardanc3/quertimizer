package com.quertimizer.user.presentation.dto.response;

import com.quertimizer.user.application.output.UserProfileSummaryOutput;
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

    public static UserProfileSummaryRes from(UserProfileSummaryOutput result) {
        return new UserProfileSummaryRes(
                result.getHandle(),
                result.getBio(),
                result.getLinks().stream()
                        .map(UserProfileLinkRes::from)
                        .toList(),
                result.getDefaultDbms(),
                result.isSqlPublic(),
                result.isExecutionPercentilePublic(),
                result.isSolvedRecordsPublic(),
                result.isSolvedProblemCountPublic(),
                result.getAverageExecutionPercentilePostgresql(),
                result.getAverageExecutionPercentileOracle(),
                result.getAuthoredPostCount(),
                result.getLikedPostCount(),
                result.getCommentCount()
        );
    }
}
