package com.quertimizer.problem.infrastructure.repository;

import com.quertimizer.problem.application.port.ProblemSolveHistoryRepository;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ProblemSolveHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemSolveHistoryJpaRepository extends JpaRepository<ProblemSolveHistory, ProblemSolveHistoryId>, ProblemSolveHistoryRepository {
}
