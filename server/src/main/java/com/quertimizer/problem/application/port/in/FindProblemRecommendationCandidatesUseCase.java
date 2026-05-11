package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.input.ProblemRecommendationCandidatesInput;
import com.quertimizer.problem.application.output.ProblemRecommendationCandidateOutput;

import java.util.List;

public interface FindProblemRecommendationCandidatesUseCase {

    List<ProblemRecommendationCandidateOutput> execute(ProblemRecommendationCandidatesInput input);

}
