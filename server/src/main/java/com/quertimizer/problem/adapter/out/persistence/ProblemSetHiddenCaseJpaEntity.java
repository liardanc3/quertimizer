package com.quertimizer.problem.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem_set_hidden_case")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemSetHiddenCaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "problem_set_id", nullable = false, length = 6)
    private String problemSetId;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "case_order", nullable = false)
    private int caseOrder;

    public static ProblemSetHiddenCaseJpaEntity create(String problemSetId, Long datasetId, int caseOrder) {
        // 문제 테이블셋 숨김 채점 케이스 JPA 엔티티 생성
        return new ProblemSetHiddenCaseJpaEntity(problemSetId, datasetId, caseOrder);
    }

    private ProblemSetHiddenCaseJpaEntity(String problemSetId, Long datasetId, int caseOrder) {
        this.problemSetId = problemSetId;
        this.datasetId = datasetId;
        this.caseOrder = caseOrder;
    }
}
