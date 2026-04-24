package com.quertimizer.problem.infrastructure.repository;

import com.quertimizer.problem.application.port.ProblemRepository;
import com.quertimizer.problem.domain.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemJpaRepository extends JpaRepository<Problem, String>, ProblemRepository {
}
