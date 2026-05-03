package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.input.ProblemOutputPreviewInput;
import com.quertimizer.problem.application.output.ProblemOutputPreviewOutput;

public interface PreviewProblemUseCase {

    ProblemOutputPreviewOutput execute(ProblemOutputPreviewInput input);
}
