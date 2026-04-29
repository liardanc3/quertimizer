package com.quertimizer.problem.application.output;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 문제 실행 데이터셋 생성 결과다.
 */
@Getter
@RequiredArgsConstructor
public class ProblemSqlDatasetOutput {

    private final String datasetId;
}
