package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.input.ProblemUserSubmitCountInput;
import com.quertimizer.problem.application.output.ProblemUserSubmitCountOutput;
import org.springframework.data.domain.Page;

public interface GetProblemUserSubmitCountsUseCase {

    Page<ProblemUserSubmitCountOutput> execute(ProblemUserSubmitCountInput input);
}
