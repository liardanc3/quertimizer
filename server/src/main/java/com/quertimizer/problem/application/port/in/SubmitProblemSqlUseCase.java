package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.input.ProblemSubmissionInput;
import com.quertimizer.problem.application.output.ProblemSubmissionOutput;

public interface SubmitProblemSqlUseCase {

    ProblemSubmissionOutput execute(ProblemSubmissionInput input);
}
