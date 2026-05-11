package com.quertimizer.problem.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemJpaRepository extends JpaRepository<ProblemJpaEntity, Long> {
    Optional<ProblemJpaEntity> findByProblemId(String problemId);
    List<ProblemJpaEntity> findAllByProblemSetIdOrderByProblemIdAsc(String problemSetId);
    Optional<ProblemJpaEntity> findFirstByProblemSetIdOrderByProblemIdDesc(String problemSetId);
}
