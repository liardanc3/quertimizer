package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.domain.model.ProblemAnswerCaseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem_answer_case")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemAnswerCaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "problem_id", nullable = false, length = 12)
    private String problemId;

    @Column(name = "dataset_id", nullable = false, length = 80)
    private String datasetId;

    @Column(name = "answer_hash", nullable = false, columnDefinition = "TEXT")
    private String answerHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, length = 20)
    private ProblemAnswerCaseType caseType;

    @Column(name = "case_order", nullable = false)
    private int caseOrder;

    public static ProblemAnswerCaseJpaEntity create(String problemId, String datasetId,
                                                    String answerHash, ProblemAnswerCaseType caseType,
                                                    int caseOrder) {
        // 문제 정답 케이스 JPA 엔티티 생성
        return new ProblemAnswerCaseJpaEntity(problemId, datasetId, answerHash, caseType, caseOrder);
    }

    private ProblemAnswerCaseJpaEntity(String problemId, String datasetId,
                                       String answerHash, ProblemAnswerCaseType caseType,
                                       int caseOrder) {
        this.problemId = problemId;
        this.datasetId = datasetId;
        this.answerHash = answerHash;
        this.caseType = caseType;
        this.caseOrder = caseOrder;
    }
}
