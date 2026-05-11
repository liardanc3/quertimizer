package com.quertimizer.problem.application.port.out;

import com.quertimizer.problem.domain.entity.ProblemAnswerCase;

import java.util.List;
import java.util.Optional;

public interface ProblemAnswerCaseRepositoryPort {

    Optional<ProblemAnswerCase> findActualByProblemId(String problemId);

    List<ProblemAnswerCase> findHiddenByProblemIdOrderByCaseOrderAsc(String problemId);

    List<ProblemAnswerCase> saveAll(List<ProblemAnswerCase> answerCases);

    void deleteAllByProblemId(String problemId);
}
