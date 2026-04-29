package com.quertimizer.problem.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 등록된 데이터셋의 기준 SQL을 생성하기 위한 입력이다.
 */
@Getter
@RequiredArgsConstructor
public class ProblemSqlReferenceInput {

    private final String datasetId;
    private final String referenceSql;
}
