package com.quertimizer.repository;

import com.quertimizer.entity.ProblemSolveHistory;
import com.quertimizer.entity.ProblemSolveHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemSolveHistoryRepository extends JpaRepository<ProblemSolveHistory, ProblemSolveHistoryId> {

    List<ProblemSolveHistory> findAllByUserIdOrderBySubmittedAtDesc(String userId);

}
