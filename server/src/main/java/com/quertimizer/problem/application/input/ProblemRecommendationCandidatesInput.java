package com.quertimizer.problem.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

@Data
public class ProblemRecommendationCandidatesInput {

    private final DbmsType dbmsType;
    private final String currentHandle;
    private final int candidateLimit;

}
