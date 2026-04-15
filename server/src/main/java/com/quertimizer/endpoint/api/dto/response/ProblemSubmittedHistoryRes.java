package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.constant.ExecutionPlanElementIndexes;
import com.quertimizer.entity.ProblemSolveHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProblemSubmittedHistoryRes {

    private final String dbms;
    private final String userId;
    private final long executionPlanElement;
    private final long executionTimeMs;
    private final double cost;

    public static ProblemSubmittedHistoryRes from(ProblemSolveHistory history) {

        // DBMS 기준 실행계획 비트 정규화
        DbmsType dbmsType = resolveDbmsType(history);

        return new ProblemSubmittedHistoryRes(
                dbmsType.getValue(),
                history.getUserId(),
                ExecutionPlanElementIndexes.normalize(dbmsType, history.getExecutionPlanElement()),
                history.getExecutionTimeMs(),
                history.getCost()
        );
    }

    private static DbmsType resolveDbmsType(ProblemSolveHistory history) {
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

}
