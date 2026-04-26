package com.quertimizer.judge.application.port;

import com.quertimizer.judge.application.input.GenerateAnswerHashInput;
import com.quertimizer.judge.application.input.ProblemOutputPreviewInput;
import com.quertimizer.judge.application.output.ProblemOutputPreviewOutput;

public interface JudgeExecutionOrchestratorPort {

    ProblemOutputPreviewOutput executeProblemOutputPreview(ProblemOutputPreviewInput input);

    ProblemOutputPreviewOutput executeAnswerHashSource(GenerateAnswerHashInput input);
}
