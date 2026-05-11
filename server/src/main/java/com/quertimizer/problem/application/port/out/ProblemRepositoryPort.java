package com.quertimizer.problem.application.port.out;

import com.quertimizer.problem.domain.entity.Problem;

import java.util.List;
import java.util.Optional;

public interface ProblemRepositoryPort {

    List<Problem> findAll();

    List<Problem> findAllByProblemSetIdOrderByProblemIdAsc(String problemSetId);

    Optional<Problem> findByProblemId(String problemId);

    Optional<String> findLatestProblemIdByProblemSetId(String problemSetId);

    Problem save(Problem problem);
}
