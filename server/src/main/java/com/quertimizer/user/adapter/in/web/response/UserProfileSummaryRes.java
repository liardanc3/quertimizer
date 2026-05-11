package com.quertimizer.user.adapter.in.web.response;

import com.quertimizer.user.application.output.UserProfileSummaryOutput;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserProfileSummaryRes {

    private final String handle;
    private final String bio;
    private final String profileImageUrl;
    private final String backgroundImageUrl;
    private final LocalDateTime signupAt;
    private final List<UserProfileLinkRes> links;
    private final String defaultDbms;
    private final boolean sqlPublic;
    private final boolean executionPercentilePublic;
    private final boolean solvedRecordsPublic;
    private final boolean solvedProblemCountPublic;
    private final boolean communityActivityPublic;
    private final Double averageExecutionPercentilePostgresql;
    private final Double averageExecutionPercentileMysql;
    private final long authoredPostCount;
    private final long likedPostCount;
    private final long commentCount;

    public static UserProfileSummaryRes from(UserProfileSummaryOutput result) {
        return new UserProfileSummaryRes(
                result.getHandle(),
                result.getBio(),
                result.getProfileImageUrl(),
                result.getBackgroundImageUrl(),
                result.getSignupAt(),
                result.getLinks().stream()
                        .map(UserProfileLinkRes::from)
                        .toList(),
                result.getDefaultDbms(),
                result.isSqlPublic(),
                result.isExecutionPercentilePublic(),
                result.isSolvedRecordsPublic(),
                result.isSolvedProblemCountPublic(),
                result.isCommunityActivityPublic(),
                result.getAverageExecutionPercentilePostgresql(),
                result.getAverageExecutionPercentileMysql(),
                result.getAuthoredPostCount(),
                result.getLikedPostCount(),
                result.getCommentCount()
        );
    }
}
