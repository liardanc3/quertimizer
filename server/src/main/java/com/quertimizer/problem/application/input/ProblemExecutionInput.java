package com.quertimizer.problem.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemExecutionProgress;
import lombok.Data;

import java.util.List;
import java.util.function.Consumer;

@Data
public class ProblemExecutionInput {

    private final String handle;
    private final String executionSessionId;
    private final String problemId;
    private final String sql;
    private final DbmsType dbmsType;
    private final Integer page;
    private final Integer pageSize;
    private final List<String> indexSqls;
    private final Consumer<ProblemExecutionProgress> progressListener;

    public static ProblemExecutionInput of(String handle, String executionSessionId,
                                           String problemId, String sql, String dbms,
                                           Integer page, Integer pageSize,
                                           List<String> indexSqls) {
        return of(handle, executionSessionId, problemId, sql, dbms, page, pageSize, indexSqls, progress -> {
        });
    }

    public static ProblemExecutionInput of(String handle, String executionSessionId,
                                           String problemId, String sql, String dbms,
                                           Integer page, Integer pageSize, List<String> indexSqls,
                                           Consumer<ProblemExecutionProgress> progressListener) {
        // 정리된 요청 값과 실행 전 반영할 index DDL 목록을 애플리케이션 실행 입력으로 변환
        return new ProblemExecutionInput(
                handle, executionSessionId,
                problemId, sql, DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL),
                page, pageSize, normalizeIndexSqls(indexSqls),
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

    private ProblemExecutionInput(String handle, String executionSessionId, String problemId, String sql,
                                  DbmsType dbmsType, Integer page, Integer pageSize, List<String> indexSqls,
                                  Consumer<ProblemExecutionProgress> progressListener) {
        this.handle = handle;
        this.executionSessionId = executionSessionId;
        this.problemId = problemId;
        this.sql = sql;
        this.dbmsType = dbmsType;
        this.page = page;
        this.pageSize = pageSize;
        this.indexSqls = indexSqls;
        this.progressListener = progressListener;
    }
}
