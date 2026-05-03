package com.quertimizer.problem.application.port.out;

import com.quertimizer.problem.domain.entity.ProblemSet;

import java.util.List;
import java.util.Optional;

public interface ProblemSetRepositoryPort {

    List<ProblemSet> findAll();

    Optional<ProblemSet> findByProblemSetId(String problemSetId);

    ProblemSet save(ProblemSet problemSet);
}
