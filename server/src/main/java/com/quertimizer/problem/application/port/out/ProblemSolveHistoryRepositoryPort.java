package com.quertimizer.problem.application.port.out;

import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ids.ProblemSolveHistoryId;

import java.util.List;
import java.util.Optional;

public interface ProblemSolveHistoryRepositoryPort {

    List<ProblemSolveHistory> findAll();

    List<ProblemSolveHistory> findAllByProblemId(String problemId);

    List<ProblemSolveHistory> findAllByHandleOrderBySubmittedAtDesc(String handle);

    Optional<ProblemSolveHistory> findById(ProblemSolveHistoryId problemSolveHistoryId);

    ProblemSolveHistory save(ProblemSolveHistory problemSolveHistory);
}
