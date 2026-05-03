package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.output.AdminProblemOptionOutput;
import com.quertimizer.problem.domain.entity.Problem;
import java.util.List;

public interface GetProblemOptionsUseCase {

    List<AdminProblemOptionOutput> execute(String problemSetId);
}
