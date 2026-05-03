package com.quertimizer.problem.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

    public static ProblemExecutionInput of(String handle, String executionSessionId,
                                           String problemId, String sql, String dbms,
                                           Integer page, Integer pageSize) {
        // 정리된 요청 값을 애플리케이션 실행 입력으로 변환
        return new ProblemExecutionInput(
                handle, executionSessionId,
                problemId, sql, DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL),
                page, pageSize
        );
    }
}
