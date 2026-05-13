package com.quertimizer.problem.adapter.in.websocket.dto;

import com.quertimizer.problem.application.output.ProblemSubmissionProgress;
import lombok.Data;

import java.util.List;

@Data
public class ProblemSubmitProgressRes {

    private final String type;
    private final String problemId;
    private final String stepKey;
    private final String status;
    private final String message;
    private final List<String> detailLines;
    private final String statementKey;
    private final Integer statementIndex;
    private final String statementSql;
    private final String statementMode;
    private final Boolean statementReference;

    public ProblemSubmitProgressRes(String type, String problemId, String stepKey, String status, String message,
                                    List<String> detailLines, String statementKey, Integer statementIndex,
                                    String statementSql, String statementMode, Boolean statementReference) {
        this.type = type;
        this.problemId = problemId;
        this.stepKey = stepKey;
        this.status = status;
        this.message = message;
        this.detailLines = detailLines;
        this.statementKey = statementKey;
        this.statementIndex = statementIndex;
        this.statementSql = statementSql;
        this.statementMode = statementMode;
        this.statementReference = statementReference;
    }

    public static ProblemSubmitProgressRes of(String problemId,
                                              String stepKey,
                                              String status,
                                              String message,
                                              List<String> detailLines,
                                              String statementKey,
                                              Integer statementIndex,
                                              String statementSql,
                                              String statementMode,
                                              Boolean statementReference) {
        return new ProblemSubmitProgressRes(
                "problem.submit.progress",
                problemId,
                stepKey,
                status,
                message,
                detailLines != null ? detailLines : List.of(),
                statementKey,
                statementIndex,
                statementSql,
                statementMode,
                statementReference
        );
    }

    public static ProblemSubmitProgressRes from(ProblemSubmissionProgress progress) {
        return of(
                progress.getProblemId(), progress.getStepKey(), progress.getStatus(), progress.getMessage(),
                progress.getDetailLines(), progress.getStatementKey(), progress.getStatementIndex(),
                progress.getStatementSql(), progress.getStatementMode(), progress.getStatementReference()
        );
    }
}
