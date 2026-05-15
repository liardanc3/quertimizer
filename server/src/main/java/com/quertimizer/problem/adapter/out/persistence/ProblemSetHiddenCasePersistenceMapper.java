package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.domain.entity.ProblemSetHiddenCase;
import org.springframework.stereotype.Component;

@Component
public class ProblemSetHiddenCasePersistenceMapper {

    public ProblemSetHiddenCase toDomain(ProblemSetHiddenCaseJpaEntity entity) {
        // 문제 테이블셋 숨김 채점 케이스 JPA 엔티티를 도메인 엔티티로 변환
        return ProblemSetHiddenCase.restore(entity.getId(), entity.getProblemSetId(), entity.getDatasetId(), entity.getCaseOrder());
    }

    public ProblemSetHiddenCaseJpaEntity toEntity(ProblemSetHiddenCase hiddenCase) {
        // 문제 테이블셋 숨김 채점 케이스 도메인 엔티티를 JPA 엔티티로 변환
        return ProblemSetHiddenCaseJpaEntity.create(
                hiddenCase.getProblemSetId(), hiddenCase.getDatasetId(), hiddenCase.getCaseOrder()
        );
    }
}
