package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.constant.ExecutionPlanElementIndexes;
import com.quertimizer.entity.ProblemSubmitHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubmitHistoryListItemRes {

    private static final int SUBMIT_ID_LENGTH = 8;

    private final String submitId;
    private final String userId;
    private final String dbms;
    private final String problemId;
    private final String submittedAt;
    private final boolean success;
    private final String message;
    private final String submittedSql;
    private final double cost;
    private final long executionPlanElement;

    public static SubmitHistoryListItemRes from(ProblemSubmitHistory history) {
        DbmsType dbmsType = history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
        long executionPlanElement = history.getExecutionPlanElement() != null ? history.getExecutionPlanElement() : 0L;

        return new SubmitHistoryListItemRes(
                formatSubmitId(history.getSubmitId()),
                history.getUserId(),
                dbmsType.getValue(),
                history.getProblemId(),
                history.getSubmittedAt() != null ? history.getSubmittedAt().toString() : "",
                history.isSuccess(),
                history.getMessage() != null ? history.getMessage() : "",
                history.getSubmittedSql() != null ? history.getSubmittedSql() : "",
                history.getCost(),
                ExecutionPlanElementIndexes.normalize(dbmsType, executionPlanElement)
        );
    }

    private static String formatSubmitId(Long submitId) {
        long resolvedSubmitId = submitId != null ? submitId : 0L;
        return String.format("%0" + SUBMIT_ID_LENGTH + "d", resolvedSubmitId);
    }

}
