package com.quertimizer.problem.infrastructure.sqljudge;

import com.quertimizer.problem.application.input.ProblemSqlDatasetInput;
import com.quertimizer.problem.application.input.ProblemSqlExecutionInput;
import com.quertimizer.problem.application.input.ProblemSqlReferenceInput;
import com.quertimizer.problem.application.output.ProblemSqlDatasetOutput;
import com.quertimizer.problem.application.output.ProblemSqlExecutionOutput;
import com.quertimizer.problem.application.output.ProblemSqlReferenceOutput;
import com.quertimizer.problem.application.port.ProblemSqlJudgePort;
import com.quertimizer.sqljudge.api.SqlJudge;
import com.quertimizer.sqljudge.command.CreateDatasetCommand;
import com.quertimizer.sqljudge.command.CreateReferenceCommand;
import com.quertimizer.sqljudge.command.IsolatedExecuteCommand;
import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeExecutionId;
import com.quertimizer.sqljudge.policy.ExecutionOptions;
import com.quertimizer.sqljudge.policy.IsolationPolicy;
import com.quertimizer.sqljudge.result.SqlExecutionResult;
import com.quertimizer.sqljudge.result.SqlReferenceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 문제 도메인의 SQL 요청을 sql-judge API 호출로 변환하는 어댑터다.
 */
@Component
@RequiredArgsConstructor
public class ProblemSqlJudgeAdapter implements ProblemSqlJudgePort {

    private final SqlJudge sqlJudge;

    /**
     * 문제 도메인의 DDL과 데이터 SQL을 sql-judge 데이터셋으로 등록한다.
     *
     * @param input 데이터셋 생성 입력
     * @return 데이터셋 생성 결과
     */
    @Override
    public ProblemSqlDatasetOutput createDataset(ProblemSqlDatasetInput input) {
        // 문제 도메인의 DBMS, DDL, 데이터 SQL 입력을 sql-judge 데이터셋 생성 명령으로 변환한다.
        JudgeDatasetId datasetId = sqlJudge.createDataset(new CreateDatasetCommand(
                toSqlJudgeDbmsType(input.getDbmsType()),
                input.getDdl(),
                input.getDataSql(),
                input.getBaseIndexDdls()
        ));

        // Quertimizer는 데이터셋 내부 구조가 아니라 이후 호출에 사용할 키만 보관한다.
        return new ProblemSqlDatasetOutput(datasetId.getValue());
    }

    /**
     * 등록된 데이터셋에서 SQL을 독립 실행한다.
     *
     * @param input SQL 실행 입력
     * @return SQL 실행 결과
     */
    @Override
    public ProblemSqlExecutionOutput execute(ProblemSqlExecutionInput input) {
        // 문제 생성 중 예시 출력만 확인할 수 있도록 임시 실행 식별자와 격리 실행 정책을 부여한다.
        SqlExecutionResult result = sqlJudge.executeIsolated(new IsolatedExecuteCommand(
                new JudgeExecutionId("problem-create-" + UUID.randomUUID()),
                new JudgeDatasetId(input.getDatasetId()),
                List.of(),
                input.getSql(),
                IsolationPolicy.cleanRoom(),
                ExecutionOptions.officialCost()
        ));

        // 실행 환경 정보는 노출하지 않고 화면 저장에 필요한 결과 테이블만 문제 도메인으로 되돌린다.
        return new ProblemSqlExecutionOutput(result.getColumns(), result.getRows(), result.getRowCount());
    }

    /**
     * 실제 채점 데이터셋 기준 SQL을 sql-judge reference로 등록한다.
     *
     * @param input 기준 SQL 생성 입력
     * @return 기준 SQL 생성 결과
     */
    @Override
    public ProblemSqlReferenceOutput createReference(ProblemSqlReferenceInput input) {
        // 기준 SQL 원문과 결과 해시 생성은 sql-judge에 맡기고, 문제 도메인은 키와 해시만 받는다.
        SqlReferenceResult result = sqlJudge.createReference(new CreateReferenceCommand(
                new JudgeDatasetId(input.getDatasetId()),
                input.getReferenceSql(),
                ExecutionOptions.officialCost()
        ));

        // 이후 제출 검증은 기준 SQL 키 또는 결과 해시를 통해 이어질 수 있게 최소 정보만 반환한다.
        return new ProblemSqlReferenceOutput(result.getReferenceId().getValue(), result.getResultHash());
    }

    private com.quertimizer.sqljudge.db.DbmsType toSqlJudgeDbmsType(com.quertimizer.global.constant.DbmsType dbmsType) {
        // 외부 모듈 후보인 sql-judge가 전역 열거형에 직접 묶이지 않도록 타입을 경계에서 변환한다.
        return switch (dbmsType) {
            case POSTGRESQL -> com.quertimizer.sqljudge.db.DbmsType.POSTGRESQL;
            case MYSQL -> com.quertimizer.sqljudge.db.DbmsType.MYSQL;
        };
    }
}
