package com.quertimizer.problem.infrastructure.repository;

import com.quertimizer.problem.domain.entity.ProblemSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemSetRepository extends JpaRepository<ProblemSet, String> {
}
