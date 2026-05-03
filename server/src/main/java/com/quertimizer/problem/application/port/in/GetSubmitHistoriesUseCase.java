package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.input.SubmitHistorySearchInput;
import com.quertimizer.problem.application.output.SubmitHistoryPageOutput;

public interface GetSubmitHistoriesUseCase {

    SubmitHistoryPageOutput execute(SubmitHistorySearchInput input);
}
