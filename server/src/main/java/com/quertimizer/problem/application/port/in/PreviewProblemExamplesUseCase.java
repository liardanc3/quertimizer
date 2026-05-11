package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.input.ProblemExamplePreviewInput;
import com.quertimizer.problem.application.output.ProblemExamplePreviewOutput;

public interface PreviewProblemExamplesUseCase {

    ProblemExamplePreviewOutput execute(ProblemExamplePreviewInput input);
}
