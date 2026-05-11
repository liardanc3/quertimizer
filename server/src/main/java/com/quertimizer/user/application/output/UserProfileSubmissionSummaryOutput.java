package com.quertimizer.user.application.output;

import lombok.Data;

import java.util.List;

@Data
public class UserProfileSubmissionSummaryOutput {

    private final List<String> attemptedProblemIds;
    private final List<UserProfileSubmissionActivityOutput> submissionActivities;
}
