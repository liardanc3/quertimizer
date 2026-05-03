package com.quertimizer.problem.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import com.quertimizer.problem.application.port.out.ProblemSetRepositoryPort;
import com.quertimizer.problem.domain.entity.ProblemSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemSetPersistenceAdapter implements ProblemSetRepositoryPort {

    private final ProblemSetJpaRepository problemSetJpaRepository;
    private final ProblemSetPersistenceMapper problemSetPersistenceMapper;

    @Override
    public List<ProblemSet> findAll() {
        return problemSetJpaRepository.findAll().stream()
                .map(problemSetPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ProblemSet> findByProblemSetId(String problemSetId) {
        return problemSetJpaRepository.findByProblemSetId(problemSetId)
                .map(problemSetPersistenceMapper::toDomain);
    }

    @Override
    public ProblemSet save(ProblemSet problemSet) {
        ProblemSetJpaEntity savedEntity = problemSetJpaRepository.findByProblemSetId(problemSet.getProblemSetId())
                .map(entity -> {
                    problemSetPersistenceMapper.updateEntity(entity, problemSet);
                    return entity;
                })
                .orElseGet(() -> problemSetPersistenceMapper.toEntity(problemSet));
        savedEntity = problemSetJpaRepository.saveAndFlush(savedEntity);
        savedEntity.assignProblemSetId();
        return problemSetPersistenceMapper.toDomain(problemSetJpaRepository.saveAndFlush(savedEntity));
    }
}
