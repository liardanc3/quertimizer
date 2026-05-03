package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import org.springframework.stereotype.Component;

@Component
public class ProblemSolveHistoryPersistenceMapper {

    public ProblemSolveHistory toDomain(ProblemSolveHistoryJpaEntity entity) {
        // 문제 최고 기록 JPA 엔티티를 도메인 엔티티로 변환
        return ProblemSolveHistory.restore(
                entity.getProblemId(), entity.getHandle(), entity.getDbmsType(),
                entity.getSubmittedSql(), entity.getExecutionTimeMs(),
                entity.getCost(), entity.getScanRows(),
                entity.getExecutionPlanElement(), entity.getSubmittedAt()
        );
    }

    public ProblemSolveHistoryJpaEntity toEntity(ProblemSolveHistory solveHistory) {
        // 문제 최고 기록 도메인 엔티티를 JPA 엔티티로 변환
        return ProblemSolveHistoryJpaEntity.create(
                solveHistory.getProblemId(), solveHistory.getHandle(),
                solveHistory.getDbmsType(), solveHistory.getSubmittedSql(),
                solveHistory.getExecutionTimeMs(), solveHistory.getCost(),
                solveHistory.getScanRows(), solveHistory.getExecutionPlanElement(),
                solveHistory.getSubmittedAt()
        );
    }

    public void updateEntity(ProblemSolveHistoryJpaEntity entity, ProblemSolveHistory solveHistory) {
        // 문제 최고 기록 도메인 상태를 기존 JPA 엔티티에 반영
        entity.update(
                solveHistory.getDbmsType(), solveHistory.getSubmittedSql(),
                solveHistory.getExecutionTimeMs(), solveHistory.getCost(),
                solveHistory.getScanRows(), solveHistory.getExecutionPlanElement(),
                solveHistory.getSubmittedAt()
        );
    }
}
