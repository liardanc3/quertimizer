package com.quertimizer.problem.application.output;

import lombok.Data;

import java.util.List;

@Data
public class ProblemUserProfileOutput {

    private final int solvedCount;
    private final long totalExecutionTimeMs;
    private final Double postgresqlAverageExecutionPercentile;
    private final Double mysqlAverageExecutionPercentile;
    private final List<String> solvedProblemIds;
    private final List<ProblemUserSolvedRecordOutput> solvedRecords;
    private final List<String> attemptedProblemIds;
    private final List<ProblemUserSubmissionActivityOutput> submissionActivities;
}
