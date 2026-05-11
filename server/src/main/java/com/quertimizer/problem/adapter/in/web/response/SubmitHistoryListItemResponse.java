package com.quertimizer.problem.adapter.in.web.response;

import com.quertimizer.problem.application.output.SubmitHistoryListItemOutput;
import lombok.Data;

@Data
public class SubmitHistoryListItemResponse {

    private final String submitId;
    private final String handle;
    private final String dbms;
    private final String problemId;
    private final String submittedAt;
    private final boolean success;
    private final String message;
    private final String submittedSql;
    private final double cost;
    private final long executionPlanElement;

    public static SubmitHistoryListItemResponse from(SubmitHistoryListItemOutput result) {
        return new SubmitHistoryListItemResponse(
                result.submitId(),
                result.handle(),
                result.dbms(),
                result.problemId(),
                result.submittedAt(),
                result.success(),
                result.message(),
                result.submittedSql(),
                result.cost(),
                result.executionPlanElement()
        );
    }

}
