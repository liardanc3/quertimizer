package com.quertimizer.problem.domain.policy;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 문제 정답 해시와 SQL 실행 결과의 일치 여부를 판단한다.
 */
@Component
public class ProblemAnswerPolicy {

    /**
     * 등록된 정답 해시가 실행 결과와 일치하는지 확인한다.
     *
     * @param answerHash 문제에 등록된 정답 해시
     * @param columns SQL 실행 결과 컬럼 목록
     * @param rows SQL 실행 결과 행 목록
     * @return 정답 일치 여부
     */
    public boolean matches(String answerHash, List<String> columns, List<List<String>> rows) {
        // 정답 해시가 없으면 비교 대상이 아니므로 불일치 처리
        if (answerHash == null || answerHash.isBlank()) {
            return false;
        }

        // 현재 canonical 해시와 기존 행 집합 해시를 모두 허용해 기존 데이터 호환성 유지
        return answerHash.equalsIgnoreCase(ProblemAnswerHashSupport.hashResult(columns, rows))
                || answerHash.equalsIgnoreCase(ProblemAnswerHashSupport.hashRows(rows));
    }
}
