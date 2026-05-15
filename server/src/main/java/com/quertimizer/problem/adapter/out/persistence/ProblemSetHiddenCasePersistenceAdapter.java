package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.application.port.out.ProblemSetHiddenCaseRepositoryPort;
import com.quertimizer.problem.domain.entity.ProblemSetHiddenCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProblemSetHiddenCasePersistenceAdapter implements ProblemSetHiddenCaseRepositoryPort {

    private final ProblemSetHiddenCaseJpaRepository problemSetHiddenCaseJpaRepository;
    private final ProblemSetHiddenCasePersistenceMapper problemSetHiddenCasePersistenceMapper;

    @Override
    public List<ProblemSetHiddenCase> findAllByProblemSetIdOrderByCaseOrderAsc(String problemSetId) {
        // 문제 테이블셋 번호 기준 숨김 채점 케이스 목록 조회
        return problemSetHiddenCaseJpaRepository.findAllByProblemSetIdOrderByCaseOrderAsc(problemSetId).stream()
                .map(problemSetHiddenCasePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProblemSetHiddenCase> saveAll(List<ProblemSetHiddenCase> hiddenCases) {
        // 문제 테이블셋 숨김 채점 케이스 목록 저장 후 도메인 엔티티 반환
        return problemSetHiddenCaseJpaRepository.saveAll(hiddenCases.stream()
                        .map(problemSetHiddenCasePersistenceMapper::toEntity)
                        .toList()
                ).stream()
                .map(problemSetHiddenCasePersistenceMapper::toDomain)
                .toList();
    }
}
