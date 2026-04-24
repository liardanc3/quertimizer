package com.quertimizer.problem.application.port;

import com.quertimizer.problem.domain.entity.ProblemSet;

import java.util.List;
import java.util.Optional;

public interface ProblemSetRepository {

    List<ProblemSet> findAll();

    List<ProblemSet> findAllById(Iterable<String> problemSetIds);

    Optional<ProblemSet> findById(String problemSetId);

    <S extends ProblemSet> S save(S problemSet);
}
