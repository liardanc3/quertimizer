package com.quertimizer.problem.application.usecase;

import com.quertimizer.judge.application.input.CreateJudgeDatasetInput;
import com.quertimizer.judge.application.input.ExecuteIsolatedJudgeSqlInput;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.JudgePort;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import com.quertimizer.judge.domain.model.ExecutionOptions;
import com.quertimizer.judge.domain.model.IsolationPolicy;
import com.quertimizer.problem.application.input.ProblemOutputPreviewInput;
import com.quertimizer.problem.application.output.ProblemOutputPreviewOutput;
import com.quertimizer.problem.application.output.ProblemSqlExecutionOutput;
import com.quertimizer.problem.application.port.ProblemOutputPreviewRateLimitPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 문제 생성 화면에서 기준 SQL 결과를 미리 실행한다.
 */
@Component
@RequiredArgsConstructor
public class PreviewProblem {

    private final JudgePort judgePort;
    private final ProblemOutputPreviewRateLimitPort problemOutputPreviewRateLimitPort;

    /**
     * 문제 생성 화면의 출력 예시를 생성한다.
     *
     * <ol>
     *   <li>출력 예시 실행 요청 제한 확인
     *   <li>예시 데이터셋 등록
     *   <li>기준 SQL 실행
     *   <li>출력 예시 결과 변환
     * </ol>
     *
     * @param input 출력 예시 생성 입력
     */
    public ProblemOutputPreviewOutput execute(ProblemOutputPreviewInput input) {
        problemOutputPreviewRateLimitPort.validate(input.getRequester(), input.getClientIp());

        JudgeDatasetId datasetId = judgePort.createDataset(new CreateJudgeDatasetInput(
                toJudgeDbmsType(input.getDbmsType()), input.getDdl(), input.getSampleDataSql(), List.of()
        ));

        SqlExecutionResult result = judgePort.executeIsolated(new ExecuteIsolatedJudgeSqlInput(
                new JudgeExecutionId("problem-preview-" + UUID.randomUUID()),
                datasetId,
                List.of(),
                input.getAnswerSql(),
                IsolationPolicy.cleanRoom(),
                ExecutionOptions.officialCost()
        ));
        ProblemSqlExecutionOutput execution = new ProblemSqlExecutionOutput(
                result.getColumns(), result.getRows(), result.getRowCount()
        );

        return ProblemOutputPreviewOutput.from(execution);
    }

    private com.quertimizer.judge.domain.model.DbmsType toJudgeDbmsType(com.quertimizer.global.constant.DbmsType dbmsType) {
        // 문제 DBMS 유형을 judge DBMS 유형으로 변환
        return switch (dbmsType) {
            case POSTGRESQL -> com.quertimizer.judge.domain.model.DbmsType.POSTGRESQL;
            case MYSQL -> com.quertimizer.judge.domain.model.DbmsType.MYSQL;
        };
    }
}
