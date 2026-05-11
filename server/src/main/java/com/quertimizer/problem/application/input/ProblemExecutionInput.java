package com.quertimizer.problem.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ProblemExecutionInput {

    private final String handle;
    private final String executionSessionId;
    private final String problemId;
    private final String sql;
    private final DbmsType dbmsType;
    private final Integer page;
    private final Integer pageSize;
    private final List<String> indexSqls;

    public static ProblemExecutionInput of(String handle, String executionSessionId,
                                           String problemId, String sql, String dbms,
                                           Integer page, Integer pageSize,
                                           List<String> indexSqls) {
        // 정리된 요청 값과 실행 전 반영할 index DDL 목록을 애플리케이션 실행 입력으로 변환
        return new ProblemExecutionInput(
                handle, executionSessionId,
                problemId, sql, DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL),
                page, pageSize, normalizeIndexSqls(indexSqls)
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
