package com.quertimizer.problem.infrastructure.repository;

import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ProblemSolveHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemSolveHistoryRepository extends JpaRepository<ProblemSolveHistory, ProblemSolveHistoryId> {

    List<ProblemSolveHistory> findAllByHandleOrderBySubmittedAtDesc(String handle);

}
