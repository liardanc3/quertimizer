package com.quertimizer.problem.application.port.out;

import com.quertimizer.problem.domain.entity.ProblemSetHiddenCase;

import java.util.List;

public interface ProblemSetHiddenCaseRepositoryPort {

    List<ProblemSetHiddenCase> findAllByProblemSetIdOrderByCaseOrderAsc(String problemSetId);

    List<ProblemSetHiddenCase> saveAll(List<ProblemSetHiddenCase> hiddenCases);
}
