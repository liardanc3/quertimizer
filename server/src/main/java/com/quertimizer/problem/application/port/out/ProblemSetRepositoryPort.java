package com.quertimizer.problem.application.port.out;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.domain.entity.ProblemSet;

import java.util.List;
import java.util.Optional;

public interface ProblemSetRepositoryPort {

    List<ProblemSet> findAll();

    Optional<ProblemSet> findByProblemSetId(String problemSetId);

    Optional<String> findLatestProblemSetIdByDbmsType(DbmsType dbmsType);

    ProblemSet save(ProblemSet problemSet);
}
