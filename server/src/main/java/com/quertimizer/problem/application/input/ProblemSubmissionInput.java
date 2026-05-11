package com.quertimizer.problem.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemSubmissionProgress;
import lombok.Data;

import java.util.function.Consumer;

@Data
public class ProblemSubmissionInput {

    private final String handle;
    private final String problemId;
    private final String sql;
    private final DbmsType dbmsType;
    private final Consumer<ProblemSubmissionProgress> progressListener;

    private ProblemSubmissionInput(String handle, String problemId, String sql,
                                   DbmsType dbmsType, Consumer<ProblemSubmissionProgress> progressListener) {
        this.handle = handle;
        this.problemId = problemId;
        this.sql = sql;
        this.dbmsType = dbmsType;
        this.progressListener = progressListener;
    }

    public static ProblemSubmissionInput of(String handle, String problemId, String sql,
                                            String dbms,
                                            Consumer<ProblemSubmissionProgress> progressListener) {
        // 정리된 요청 값을 애플리케이션 제출 입력으로 변환
        return new ProblemSubmissionInput(
                handle, problemId, sql,
                DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL),
                progressListener
        );
    }
}
