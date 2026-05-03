package com.quertimizer.problem.application.port.in;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemSetDetailOutput;
import com.quertimizer.problem.domain.entity.ProblemSet;
import java.util.Optional;

public interface GetProblemSetUseCase {

    Optional<ProblemSetDetailOutput> execute(String problemSetId);
}
