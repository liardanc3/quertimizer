package com.quertimizer.problem.adapter.out.persistence;

import java.util.List;
import com.quertimizer.problem.domain.entity.ids.ProblemSolveHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemSolveHistoryJpaRepository extends JpaRepository<ProblemSolveHistoryJpaEntity, ProblemSolveHistoryId> {
    List<ProblemSolveHistoryJpaEntity> findAllByProblemId(String problemId);
    List<ProblemSolveHistoryJpaEntity> findAllByHandleOrderBySubmittedAtDesc(String handle);
}
