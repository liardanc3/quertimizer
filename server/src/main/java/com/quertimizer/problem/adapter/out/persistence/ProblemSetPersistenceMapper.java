package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.domain.entity.ProblemSet;
import org.springframework.stereotype.Component;

@Component
public class ProblemSetPersistenceMapper {

    public ProblemSet toDomain(ProblemSetJpaEntity entity) {
        // 문제 테이블셋 JPA 엔티티를 도메인 엔티티로 변환
        return ProblemSet.restore(
                entity.getId(), entity.getProblemSetId(),
                entity.getDdl(), entity.getActualDataSql(),
                entity.getDatasetId(), entity.getDbmsType()
        );
    }

    public ProblemSetJpaEntity toEntity(ProblemSet problemSet) {
        // 문제 테이블셋 도메인 엔티티를 JPA 엔티티로 변환
        return ProblemSetJpaEntity.create(
                problemSet.getProblemSetId(), problemSet.getDdl(),
                problemSet.getActualDataSql(), problemSet.getDatasetId(),
                problemSet.getDbmsType()
        );
    }

    public void updateEntity(ProblemSetJpaEntity entity, ProblemSet problemSet) {
        // 문제 테이블셋 도메인 상태를 기존 JPA 엔티티에 반영
        entity.update(
                problemSet.getDdl(), problemSet.getActualDataSql(),
                problemSet.getDatasetId(), problemSet.getDbmsType()
        );
    }
}
