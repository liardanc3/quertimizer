package com.quertimizer.user.presentation.dto.response;

import com.quertimizer.user.application.output.UserProfileSubmissionSummaryOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
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
