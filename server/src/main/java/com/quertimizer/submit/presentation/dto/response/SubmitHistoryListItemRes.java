package com.quertimizer.submit.presentation.dto.response;

import com.quertimizer.submit.application.output.SubmitHistoryListItemOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubmitHistoryListItemRes {

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

    public static SubmitHistoryListItemRes from(SubmitHistoryListItemOutput result) {
        return new SubmitHistoryListItemRes(
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
