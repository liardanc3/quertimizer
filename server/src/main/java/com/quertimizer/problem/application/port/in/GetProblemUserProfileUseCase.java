package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.output.ProblemUserProfileOutput;

public interface GetProblemUserProfileUseCase {

    ProblemUserProfileOutput execute(String handle);
}
