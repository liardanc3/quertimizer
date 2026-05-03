package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.input.ProblemCreateInput;
import com.quertimizer.problem.application.output.ProblemCreateOutput;

public interface CreateProblemUseCase {

    ProblemCreateOutput execute(ProblemCreateInput input);
}
