package com.quertimizer.problem.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProblemJpaRepository extends JpaRepository<ProblemJpaEntity, Long> {
    Optional<ProblemJpaEntity> findByProblemId(String problemId);
    List<ProblemJpaEntity> findAllByProblemSetIdOrderByProblemIdAsc(String problemSetId);
}
