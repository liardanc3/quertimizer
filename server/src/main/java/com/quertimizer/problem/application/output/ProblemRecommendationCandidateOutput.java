package com.quertimizer.problem.application.output;

import lombok.Data;

@Data
public class ProblemRecommendationCandidateOutput {

    private final String problemId;
    private final String title;
    private final String dbms;
    private final int solvedUserCount;
    private final int totalSubmitCount;
    private final int successSubmitCount;
    private final boolean solvedByCurrentUser;

}
