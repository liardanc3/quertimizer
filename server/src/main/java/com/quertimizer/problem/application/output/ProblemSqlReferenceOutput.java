package com.quertimizer.problem.application.output;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 문제 기준 SQL 생성 결과다.
 */
@Getter
@RequiredArgsConstructor
public class ProblemSqlReferenceOutput {

    private final String referenceId;
    private final String resultHash;
}
