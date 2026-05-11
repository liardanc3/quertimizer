package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.domain.entity.ProblemAnswerCase;
import org.springframework.stereotype.Component;

@Component
public class ProblemAnswerCasePersistenceMapper {

    public ProblemAnswerCase toDomain(ProblemAnswerCaseJpaEntity entity) {
        // 문제 정답 케이스 JPA 엔티티를 도메인 엔티티로 변환
        return ProblemAnswerCase.restore(
                entity.getId(), entity.getProblemId(), entity.getDatasetId(),
                entity.getAnswerHash(), entity.getCaseType(), entity.getCaseOrder()
        );
    }

    public ProblemAnswerCaseJpaEntity toEntity(ProblemAnswerCase answerCase) {
        // 문제 정답 케이스 도메인 엔티티를 JPA 엔티티로 변환
        return ProblemAnswerCaseJpaEntity.create(
                answerCase.getProblemId(), answerCase.getDatasetId(),
                answerCase.getAnswerHash(), answerCase.getCaseType(), answerCase.getCaseOrder()
        );
    }
}
