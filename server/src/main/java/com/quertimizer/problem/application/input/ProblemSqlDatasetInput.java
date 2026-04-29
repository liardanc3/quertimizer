package com.quertimizer.problem.application.input;

import com.quertimizer.global.constant.DbmsType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 문제 실행 데이터셋 생성을 위한 입력이다.
 */
@Getter
@RequiredArgsConstructor
public class ProblemSqlDatasetInput {

    private final DbmsType dbmsType;
    private final String ddl;
    private final String dataSql;
    private final List<String> baseIndexDdls;
}
