package com.quertimizer.problem.domain.entity;

import lombok.Getter;

@Getter
public class ProblemSetHiddenCase {

    private Long id;
    private String problemSetId;
    private Long datasetId;
    private int caseOrder;

    public static ProblemSetHiddenCase create(String problemSetId, Long datasetId, int caseOrder) {
        // 문제 테이블셋 숨김 채점 케이스 생성
        return new ProblemSetHiddenCase(null, problemSetId, datasetId, caseOrder);
    }

    public static ProblemSetHiddenCase restore(Long id, String problemSetId, Long datasetId, int caseOrder) {
        // 저장된 문제 테이블셋 숨김 채점 케이스 복원
        return new ProblemSetHiddenCase(id, problemSetId, datasetId, caseOrder);
    }

    private ProblemSetHiddenCase(Long id, String problemSetId, Long datasetId, int caseOrder) {
        this.id = id;
        this.problemSetId = problemSetId;
        this.datasetId = datasetId;
        this.caseOrder = caseOrder;
    }
}
