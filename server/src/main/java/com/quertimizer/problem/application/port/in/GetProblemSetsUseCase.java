package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.output.ProblemSetSummaryOutput;
import com.quertimizer.problem.domain.entity.ProblemSet;
import java.util.List;

public interface GetProblemSetsUseCase {

    List<ProblemSetSummaryOutput> execute();
}
