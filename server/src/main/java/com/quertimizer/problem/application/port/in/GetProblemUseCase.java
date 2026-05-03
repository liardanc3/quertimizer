package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.output.ProblemDetailOutput;
import com.quertimizer.problem.domain.entity.Problem;
import java.util.Optional;

public interface GetProblemUseCase {

    Optional<ProblemDetailOutput> execute(String problemId);
}
