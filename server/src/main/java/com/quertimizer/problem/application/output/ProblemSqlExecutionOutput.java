package com.quertimizer.problem.application.output;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 문제 SQL 실행 결과다.
 */
@Getter
@RequiredArgsConstructor
public class ProblemSqlExecutionOutput {

    private final List<String> columns;
    private final List<List<String>> rows;
    private final long rowCount;
}
