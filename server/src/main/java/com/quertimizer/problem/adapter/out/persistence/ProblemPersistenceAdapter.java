package com.quertimizer.problem.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import com.quertimizer.problem.application.port.out.ProblemRepositoryPort;
import com.quertimizer.problem.domain.entity.Problem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemPersistenceAdapter implements ProblemRepositoryPort {

    private final ProblemJpaRepository problemJpaRepository;
    private final ProblemPersistenceMapper problemPersistenceMapper;

    @Override
    public List<Problem> findAll() {
        return problemJpaRepository.findAll().stream()
                .map(problemPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Problem> findAllByProblemSetIdOrderByProblemIdAsc(String problemSetId) {
        return problemJpaRepository.findAllByProblemSetIdOrderByProblemIdAsc(problemSetId).stream()
                .map(problemPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Problem> findByProblemId(String problemId) {
        return problemJpaRepository.findByProblemId(problemId)
                .map(problemPersistenceMapper::toDomain);
    }

    @Override
    public Problem save(Problem problem) {
        ProblemJpaEntity savedEntity = problemJpaRepository.findByProblemId(problem.getProblemId())
                .map(entity -> {
                    problemPersistenceMapper.updateEntity(entity, problem);
                    return entity;
                })
                .orElseGet(() -> problemPersistenceMapper.toEntity(problem));
        savedEntity = problemJpaRepository.saveAndFlush(savedEntity);
        savedEntity.assignProblemId();
        return problemPersistenceMapper.toDomain(problemJpaRepository.saveAndFlush(savedEntity));
    }
}
