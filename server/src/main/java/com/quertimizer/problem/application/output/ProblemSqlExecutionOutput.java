package com.quertimizer.problem.application.output;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ProblemSqlExecutionOutput {

    private final List<String> columns;
    private final List<List<String>> rows;
    private final long rowCount;
}
