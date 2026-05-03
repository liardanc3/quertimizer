package com.quertimizer.problem.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProblemSetJpaRepository extends JpaRepository<ProblemSetJpaEntity, Long> {
    Optional<ProblemSetJpaEntity> findByProblemSetId(String problemSetId);
}
