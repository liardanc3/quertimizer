package com.quertimizer.problem.domain.policy;


import java.util.List;

public class ProblemAnswerPolicy {
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
