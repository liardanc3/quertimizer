package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileSubmissionSummaryOutput {

    private final List<String> attemptedProblemIds;
    private final List<UserProfileSubmissionActivityOutput> submissionActivities;
}
