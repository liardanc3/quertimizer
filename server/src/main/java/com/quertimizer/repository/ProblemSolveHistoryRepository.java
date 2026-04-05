package com.quertimizer.repository;

import com.quertimizer.entity.ProblemSolveHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemSolveHistoryRepository extends JpaRepository<ProblemSolveHistory, Long> {
}
