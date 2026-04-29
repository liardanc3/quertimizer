package com.quertimizer.problem.application.port;

import com.quertimizer.problem.application.input.ProblemSqlDatasetInput;
import com.quertimizer.problem.application.input.ProblemSqlExecutionInput;
import com.quertimizer.problem.application.input.ProblemSqlReferenceInput;
import com.quertimizer.problem.application.output.ProblemSqlDatasetOutput;
import com.quertimizer.problem.application.output.ProblemSqlExecutionOutput;
import com.quertimizer.problem.application.output.ProblemSqlReferenceOutput;

/**
 * 문제 도메인이 SQL 실행기를 호출하기 위한 포트다.
 */
public interface ProblemSqlJudgePort {

    /**
     * 문제 실행 데이터셋을 생성한다.
     *
     * @param input 데이터셋 생성 입력
     * @return 데이터셋 생성 결과
     */
    ProblemSqlDatasetOutput createDataset(ProblemSqlDatasetInput input);

    /**
     * 등록된 데이터셋에서 SQL을 실행한다.
     *
     * @param input SQL 실행 입력
     * @return SQL 실행 결과
     */
    ProblemSqlExecutionOutput execute(ProblemSqlExecutionInput input);

    /**
     * 등록된 데이터셋의 기준 SQL을 생성한다.
     *
     * @param input 기준 SQL 생성 입력
     * @return 기준 SQL 생성 결과
     */
    ProblemSqlReferenceOutput createReference(ProblemSqlReferenceInput input);
}
