package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.domain.entity.Problem;
import org.springframework.stereotype.Component;

@Component
public class ProblemPersistenceMapper {

    public Problem toDomain(ProblemJpaEntity entity) {
        // 문제 JPA 엔티티를 도메인 엔티티로 변환
        return Problem.restore(
                entity.getId(), entity.getProblemId(), entity.getProblemSetId(),
                entity.getTitle(), entity.getDescription(), entity.getDdl(),
                entity.getDbmsType(), entity.getCondition(), entity.getOutput(),
                entity.getSampleDataSql(), entity.getSampleOutput(),
                entity.getAnswerHash(), entity.getAnswerSql()
        );
    }

    public ProblemJpaEntity toEntity(Problem problem) {
        // 문제 도메인 엔티티를 JPA 엔티티로 변환
        return ProblemJpaEntity.create(
                problem.getProblemId(), problem.getProblemSetId(),
                problem.getTitle(), problem.getDescription(), problem.getDdl(),
                problem.getDbmsType(), problem.getCondition(), problem.getOutput(),
                problem.getSampleDataSql(), problem.getSampleOutput(),
                problem.getAnswerHash(), problem.getAnswerSql()
        );
    }

    public void updateEntity(ProblemJpaEntity entity, Problem problem) {
        // 문제 도메인 상태를 기존 JPA 엔티티에 반영
        entity.update(
                problem.getTitle(), problem.getDescription(),
                problem.getDdl(), problem.getDbmsType(),
                problem.getCondition(), problem.getOutput(),
                problem.getSampleDataSql(), problem.getSampleOutput(),
                problem.getAnswerHash(), problem.getAnswerSql()
        );
    }
}
