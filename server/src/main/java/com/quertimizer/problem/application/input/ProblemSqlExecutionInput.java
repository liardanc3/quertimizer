package com.quertimizer.problem.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 등록된 데이터셋에서 SQL을 실행하기 위한 입력이다.
 */
@Getter
@RequiredArgsConstructor
public class ProblemSqlExecutionInput {

    private final String datasetId;
    private final String sql;
}
