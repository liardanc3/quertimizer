package com.quertimizer.problem.infrastructure.repository;

import com.quertimizer.problem.application.port.ProblemSetRepository;
import com.quertimizer.problem.domain.entity.ProblemSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemSetJpaRepository extends JpaRepository<ProblemSet, String>, ProblemSetRepository {
}
