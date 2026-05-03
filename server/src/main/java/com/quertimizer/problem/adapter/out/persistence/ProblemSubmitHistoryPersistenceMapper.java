package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import org.springframework.stereotype.Component;

@Component
public class ProblemSubmitHistoryPersistenceMapper {

    public ProblemSubmitHistoryJpaEntity toEntity(ProblemSubmitHistory history) {
        return new ProblemSubmitHistoryJpaEntity(
                history.getSubmitId(), history.getProblemId(), history.getHandle(), history.getDbmsType(),
                history.getSubmittedSql(), history.isSuccess(), history.getMessage(),
                history.getExecutionTimeMs(), history.getCost(), history.getRowCount(),
                history.getExecutionPlanElement(), history.getSubmittedAt()
        );
    }

    public ProblemSubmitHistory toDomain(ProblemSubmitHistoryJpaEntity entity) {
        return ProblemSubmitHistory.restore(
                entity.getSubmitId(), entity.getProblemId(), entity.getHandle(), entity.getDbmsType(),
                entity.getSubmittedSql(), entity.isSuccess(), entity.getMessage(),
                entity.getExecutionTimeMs(), entity.getCost(), entity.getRowCount(),
                entity.getExecutionPlanElement(), entity.getSubmittedAt()
        );
    }

    public void updateEntity(ProblemSubmitHistoryJpaEntity entity, ProblemSubmitHistory history) {
        entity.updateFromDomain(history);
    }
}
