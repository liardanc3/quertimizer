package com.quertimizer.problem.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import com.quertimizer.problem.application.port.out.ProblemSolveHistoryRepositoryPort;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ids.ProblemSolveHistoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemSolveHistoryPersistenceAdapter implements ProblemSolveHistoryRepositoryPort {

    private final ProblemSolveHistoryJpaRepository problemSolveHistoryJpaRepository;
    private final ProblemSolveHistoryPersistenceMapper problemSolveHistoryPersistenceMapper;

    @Override
    public List<ProblemSolveHistory> findAll() {
        return problemSolveHistoryJpaRepository.findAll().stream()
                .map(problemSolveHistoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProblemSolveHistory> findAllByProblemId(String problemId) {
        return problemSolveHistoryJpaRepository.findAllByProblemId(problemId).stream()
                .map(problemSolveHistoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProblemSolveHistory> findAllByHandleOrderBySubmittedAtDesc(String handle) {
        return problemSolveHistoryJpaRepository.findAllByHandleOrderBySubmittedAtDesc(handle).stream()
                .map(problemSolveHistoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ProblemSolveHistory> findById(ProblemSolveHistoryId problemSolveHistoryId) {
        return problemSolveHistoryJpaRepository.findById(problemSolveHistoryId)
                .map(problemSolveHistoryPersistenceMapper::toDomain);
    }

    @Override
    public ProblemSolveHistory save(ProblemSolveHistory problemSolveHistory) {
        ProblemSolveHistoryId historyId = new ProblemSolveHistoryId(problemSolveHistory.getProblemId(), problemSolveHistory.getHandle());
        ProblemSolveHistoryJpaEntity savedEntity = problemSolveHistoryJpaRepository.findById(historyId)
                .map(entity -> {
                    problemSolveHistoryPersistenceMapper.updateEntity(entity, problemSolveHistory);
                    return entity;
                })
                .orElseGet(() -> problemSolveHistoryPersistenceMapper.toEntity(problemSolveHistory));
        return problemSolveHistoryPersistenceMapper.toDomain(problemSolveHistoryJpaRepository.save(savedEntity));
    }
}
