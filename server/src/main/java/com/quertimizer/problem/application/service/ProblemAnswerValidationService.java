package com.quertimizer.problem.application.service;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.application.port.out.ProblemAnswerCaseRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemRepositoryPort;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemAnswerCase;
import com.quertimizer.problem.domain.policy.ProblemAnswerPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.quertimizer.problem.domain.model.ProblemQueryFailReason.ANSWER_HASH_NOT_REGISTERED;
import static com.quertimizer.problem.domain.model.ProblemQueryFailReason.PROBLEM_INFO_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProblemAnswerValidationService {

    private final ProblemRepositoryPort problemRepository;
    private final ProblemAnswerCaseRepositoryPort problemAnswerCaseRepository;
    private final ProblemAnswerPolicy problemAnswerPolicy;

    public boolean isCorrectAnswer(String problemId, List<String> columns, List<List<String>> rows) {
        // 문제의 정답 해시 조회
        Problem problem = problemRepository.findByProblemId(problemId)
                .orElseThrow(() -> new BusinessException(PROBLEM_INFO_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        String answerHash = problemAnswerCaseRepository.findActualByProblemId(problemId)
                .map(ProblemAnswerCase::getAnswerHash)
                .orElse(problem.getAnswer());

        // 정답 해시가 등록되지 않은 문제는 제출 채점 대상으로 사용 불가
        if (answerHash == null || answerHash.isBlank()) {
            throw new BusinessException(ANSWER_HASH_NOT_REGISTERED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // 정답 비교 규칙은 problem 도메인 정책에 위임
        return problemAnswerPolicy.matches(answerHash, columns, rows);
    }

    public List<ProblemAnswerCase> findHiddenAnswerCases(String problemId) {
        // 문제 번호 기준 숨김 채점 정답 케이스 목록 조회
        return problemAnswerCaseRepository.findHiddenByProblemIdOrderByCaseOrderAsc(problemId);
    }

    public boolean matches(String answerHash, List<String> columns, List<List<String>> rows) {
        // 정답 비교 규칙은 problem 도메인 정책에 위임
        return problemAnswerPolicy.matches(answerHash, columns, rows);
    }
}
