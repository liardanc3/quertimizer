package com.quertimizer.user.application.port.out;

import com.quertimizer.user.application.output.UserProfileSolvedProblemsOutput;
import com.quertimizer.user.application.output.UserProfileSolvedRecordsOutput;
import com.quertimizer.user.application.output.UserProfileSubmissionSummaryOutput;
import com.quertimizer.user.domain.model.UserProfileProblemSummary;

public interface UserProfileProblemPort {

    UserProfileProblemSummary getProblemSummary(String handle);

    UserProfileSolvedProblemsOutput getSolvedProblems(String handle);

    UserProfileSolvedRecordsOutput getSolvedRecords(String handle);

    UserProfileSubmissionSummaryOutput getSubmissionSummary(String handle);

}
