package com.quertimizer.problem.application.port;

import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ids.ProblemSolveHistoryId;

import java.util.List;
import java.util.Optional;

public interface ProblemSolveHistoryRepository {

    List<ProblemSolveHistory> findAll();

    List<ProblemSolveHistory> findAllByHandleOrderBySubmittedAtDesc(String handle);

    Optional<ProblemSolveHistory> findById(ProblemSolveHistoryId problemSolveHistoryId);

    <S extends ProblemSolveHistory> S save(S problemSolveHistory);
}
