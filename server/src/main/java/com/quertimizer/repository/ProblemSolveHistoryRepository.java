package com.quertimizer.repository;

import com.quertimizer.entity.ProblemSolveHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemSolveHistoryRepository extends JpaRepository<ProblemSolveHistory, Long> {

    List<ProblemSolveHistory> findAllByUserIdOrderBySubmittedAtDesc(String userId);

}
