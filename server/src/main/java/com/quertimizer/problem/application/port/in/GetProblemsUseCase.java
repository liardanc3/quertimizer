package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.input.ProblemSearchInput;
import com.quertimizer.problem.application.output.ProblemListItemOutput;
import com.quertimizer.problem.application.output.ProblemPage;
import com.quertimizer.problem.application.output.ProblemPageOutput;

public interface GetProblemsUseCase {

    ProblemPageOutput execute(ProblemSearchInput input);
}
