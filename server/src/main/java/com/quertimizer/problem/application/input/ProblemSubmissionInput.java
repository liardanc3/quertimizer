package com.quertimizer.problem.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemSubmissionProgress;
import lombok.Data;

import java.util.List;
import java.util.function.Consumer;

@Data
public class ProblemSubmissionInput {

    private final String handle;
    private final String problemId;
    private final String sql;
    private final DbmsType dbmsType;
    private final List<String> indexSqls;
    private final Consumer<ProblemSubmissionProgress> progressListener;

    private ProblemSubmissionInput(String handle, String problemId, String sql, DbmsType dbmsType,
                                   List<String> indexSqls, Consumer<ProblemSubmissionProgress> progressListener) {
        this.handle = handle;
        this.problemId = problemId;
        this.sql = sql;
        this.dbmsType = dbmsType;
        this.indexSqls = indexSqls;
        this.progressListener = progressListener;
    }

    public static ProblemSubmissionInput of(String handle, String problemId, String sql,
                                            String dbms,
                                            Consumer<ProblemSubmissionProgress> progressListener) {
        return of(handle, problemId, sql, dbms, List.of(), progressListener);
    }

    public static ProblemSubmissionInput of(String handle, String problemId, String sql, String dbms,
                                            List<String> indexSqls,
                                            Consumer<ProblemSubmissionProgress> progressListener) {
        // 정리된 요청 값을 애플리케이션 제출 입력으로 변환
        return new ProblemSubmissionInput(
                handle, problemId, sql, DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL),
                normalizeIndexSqls(indexSqls),
                progressListener != null ? progressListener : progress -> {
                }
        );
    }

    private static List<String> normalizeIndexSqls(List<String> indexSqls) {
        // null 또는 공백 index DDL 제거
        if (indexSqls == null || indexSqls.isEmpty()) {
            return List.of();
        }

        // 실행 가능한 index DDL 문자열 목록 반환
        return indexSqls.stream()
                .filter(indexSql -> indexSql != null && !indexSql.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
