package com.quertimizer.problem.application.port;

import com.quertimizer.problem.domain.entity.Problem;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository {

    List<Problem> findAll();

    List<Problem> findAllById(Iterable<String> problemIds);

    List<Problem> findAllByProblemSetIdOrderByProblemIdAsc(String problemSetId);

    Optional<Problem> findById(String problemId);

    <S extends Problem> S save(S problem);
}
