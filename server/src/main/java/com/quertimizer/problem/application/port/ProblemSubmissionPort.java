package com.quertimizer.problem.application.port;

import com.quertimizer.problem.application.input.ProblemSubmissionInput;
import com.quertimizer.problem.application.output.ProblemSubmissionOutput;

public interface ProblemSubmissionPort {

    ProblemSubmissionOutput submit(ProblemSubmissionInput input);
}
