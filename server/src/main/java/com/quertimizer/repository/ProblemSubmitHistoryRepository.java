package com.quertimizer.repository;

import com.quertimizer.entity.ProblemSubmitHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemSubmitHistoryRepository extends JpaRepository<ProblemSubmitHistory, Long> {
}
