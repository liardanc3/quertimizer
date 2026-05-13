package com.quertimizer.user.adapter.in.http.response;

import com.quertimizer.user.application.output.UserProfileSubmissionSummaryOutput;
import lombok.Data;

import java.util.List;

@Data
public class UserProfileSubmissionSummaryRes {

    private final List<String> attemptedProblemIds;
    private final List<UserProfileSubmissionActivityRes> submissionActivities;

    public static UserProfileSubmissionSummaryRes from(UserProfileSubmissionSummaryOutput result) {
        return new UserProfileSubmissionSummaryRes(
                result.getAttemptedProblemIds(),
                result.getSubmissionActivities().stream()
                        .map(UserProfileSubmissionActivityRes::from)
                        .toList()
        );
    }
}
