package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.application.port.out.ProblemAnswerCaseRepositoryPort;
import com.quertimizer.problem.domain.entity.ProblemAnswerCase;
import com.quertimizer.problem.domain.model.ProblemAnswerCaseType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProblemAnswerCasePersistenceAdapter implements ProblemAnswerCaseRepositoryPort {

    private final ProblemAnswerCaseJpaRepository problemAnswerCaseJpaRepository;
    private final ProblemAnswerCasePersistenceMapper problemAnswerCasePersistenceMapper;

    @Override
    public Optional<ProblemAnswerCase> findActualByProblemId(String problemId) {
        // 문제 번호 기준 실제 채점 정답 케이스 조회
        return problemAnswerCaseJpaRepository.findFirstByProblemIdAndCaseTypeOrderByCaseOrderAsc(
                        problemId, ProblemAnswerCaseType.ACTUAL
                )
                .map(problemAnswerCasePersistenceMapper::toDomain);
    }

    @Override
    public List<ProblemAnswerCase> findHiddenByProblemIdOrderByCaseOrderAsc(String problemId) {
        // 문제 번호 기준 숨김 채점 정답 케이스 목록 조회
        return problemAnswerCaseJpaRepository.findAllByProblemIdAndCaseTypeOrderByCaseOrderAsc(
                        problemId, ProblemAnswerCaseType.HIDDEN
                ).stream()
                .map(problemAnswerCasePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProblemAnswerCase> saveAll(List<ProblemAnswerCase> answerCases) {
        // 문제 정답 케이스 목록 저장 후 도메인 엔티티 반환
        return problemAnswerCaseJpaRepository.saveAll(answerCases.stream()
                        .map(problemAnswerCasePersistenceMapper::toEntity)
                        .toList()
                ).stream()
                .map(problemAnswerCasePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteAllByProblemId(String problemId) {
        // 문제 번호 기준 정답 케이스 전체 삭제
        problemAnswerCaseJpaRepository.deleteAllByProblemId(problemId);
    }
}
