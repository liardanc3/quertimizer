package com.quertimizer.problem.infrastructure.repository;

import com.quertimizer.problem.domain.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, String> {

    List<Problem> findAllByProblemSetIdOrderByProblemIdAsc(String problemSetId);
}
