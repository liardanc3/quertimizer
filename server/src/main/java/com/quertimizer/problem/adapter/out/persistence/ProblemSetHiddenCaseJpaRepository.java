package com.quertimizer.problem.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemSetHiddenCaseJpaRepository extends JpaRepository<ProblemSetHiddenCaseJpaEntity, Long> {

    List<ProblemSetHiddenCaseJpaEntity> findAllByProblemSetIdOrderByCaseOrderAsc(String problemSetId);
}
