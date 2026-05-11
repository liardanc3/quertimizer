package com.quertimizer.problem.domain.entity;

import com.quertimizer.problem.domain.model.ProblemAnswerCaseType;
import lombok.Getter;

@Getter
public class ProblemAnswerCase {

    private Long id;
    private String problemId;
    private String datasetId;
    private String answerHash;
    private ProblemAnswerCaseType caseType;
    private int caseOrder;

    public static ProblemAnswerCase actual(String problemId, String datasetId, String answerHash) {
        // 실제 채점 데이터 기준 정답 케이스 생성
        return new ProblemAnswerCase(null, problemId, datasetId, answerHash, ProblemAnswerCaseType.ACTUAL, 0);
    }

    public static ProblemAnswerCase hidden(String problemId, String datasetId, String answerHash, int caseOrder) {
        // 숨김 채점 데이터 기준 정답 케이스 생성
        return new ProblemAnswerCase(null, problemId, datasetId, answerHash, ProblemAnswerCaseType.HIDDEN, caseOrder);
    }

    public static ProblemAnswerCase restore(Long id, String problemId, String datasetId,
                                            String answerHash, ProblemAnswerCaseType caseType,
                                            int caseOrder) {
        // 저장된 정답 케이스 상태 복원
        return new ProblemAnswerCase(id, problemId, datasetId, answerHash, caseType, caseOrder);
    }

    public boolean isActual() {
        // 실제 채점 케이스 여부 반환
        return caseType == ProblemAnswerCaseType.ACTUAL;
    }

    public boolean isHidden() {
        // 숨김 채점 케이스 여부 반환
        return caseType == ProblemAnswerCaseType.HIDDEN;
    }

    private ProblemAnswerCase(Long id, String problemId, String datasetId,
                              String answerHash, ProblemAnswerCaseType caseType,
                              int caseOrder) {
        this.id = id;
        this.problemId = problemId;
        this.datasetId = datasetId;
        this.answerHash = answerHash;
        this.caseType = caseType;
        this.caseOrder = caseOrder;
    }
}
