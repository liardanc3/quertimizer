package com.quertimizer.problem.adapter.out.judge;

import com.quertimizer.judge.application.input.AnalyzeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateDatasetInput;
import com.quertimizer.judge.application.input.CreateEnvironmentInput;
import com.quertimizer.judge.application.input.CreateSqlExecutionHashInput;
import com.quertimizer.judge.application.input.ExecuteSqlInput;
import com.quertimizer.judge.domain.model.ExecutionMode;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.in.JudgeApplicationPort;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import com.quertimizer.judge.domain.model.ExecutionOptions;
import com.quertimizer.judge.domain.model.QueuePriority;
import com.quertimizer.judge.domain.model.QueueStatusListener;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.output.ProblemSqlStatement;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component("problemJudgeGateway")
@RequiredArgsConstructor
public class JudgeGateway implements ProblemJudgePort {

    private final JudgeApplicationPort judgeApplicationPort;

    @Override
    public Long createDataset(DbmsType dbmsType, String ddl, String dataSql) {
        // problem SQL 자료를 채점 데이터셋으로 등록
        return judgeApplicationPort.createDataset(new CreateDatasetInput(dbmsType, ddl, dataSql, List.of())).getValue();
    }

    @Override
    public Long createInlineDataset(DbmsType dbmsType, String ddl, String dataSql) {
        // 별도 problem_set 행이 없는 SQL 자료를 채점 데이터셋 내부 정의와 함께 등록
        return judgeApplicationPort.createDataset(new CreateDatasetInput(dbmsType, ddl, dataSql, List.of(), true)).getValue();
    }

    @Override
    public Long createTemporaryDataset(DbmsType dbmsType, String ddl, String dataSql) {
        // 임시 SQL 자료를 채점 데이터셋 내부 정의와 함께 등록
        return judgeApplicationPort.createDataset(new CreateDatasetInput(dbmsType, ddl, dataSql, List.of(), true)).getValue();
    }

    @Override
    public boolean hasDataset(Long datasetId) {
        // 채점 데이터셋 존재 여부 확인
        return judgeApplicationPort.hasDataset(datasetId);
    }

    @Override
    public void deleteDataset(Long datasetId) {
        // 채점 데이터셋 제거
        judgeApplicationPort.deleteDataset(new JudgeDatasetId(datasetId));
    }

    @Override
    public String createAnswerHash(Long datasetId, String answerSql) {
        // 채점 데이터셋 기준 정답 SQL 실행 해시 생성
        return judgeApplicationPort.createSqlExecutionHash(new CreateSqlExecutionHashInput(
                new JudgeDatasetId(datasetId), answerSql, ExecutionOptions.officialCost()
        )).getResultHash();
    }

    @Override
    public String createInteractiveEnvironment(Long datasetId) {
        return createInteractiveEnvironment(datasetId, remainingTasks -> {
        });
    }

    @Override
    public String createInteractiveEnvironment(Long datasetId, Consumer<Integer> remainingTaskListener) {
        return createInteractiveEnvironment(datasetId, remainingTaskListener, detail -> {
        });
    }

    @Override
    public String createInteractiveEnvironment(Long datasetId, Consumer<Integer> remainingTaskListener,
                                               Consumer<String> detailListener) {
        // 인터랙티브 문제 실행 환경 생성
        return judgeApplicationPort.createEnvironment(new CreateEnvironmentInput(
                new JudgeDatasetId(datasetId), EnvironmentPolicy.interactive(),
                QueuePriority.NORMAL, createQueueStatusListener(remainingTaskListener, detailListener)
        )).getValue();
    }

    @Override
    public String createSubmissionEnvironment(Long datasetId) {
        return createSubmissionEnvironment(datasetId, remainingTasks -> {
        });
    }

    @Override
    public String createSubmissionEnvironment(Long datasetId, Consumer<Integer> remainingTaskListener) {
        return createSubmissionEnvironment(datasetId, remainingTaskListener, detail -> {
        });
    }

    @Override
    public String createSubmissionEnvironment(Long datasetId, Consumer<Integer> remainingTaskListener,
                                              Consumer<String> detailListener) {
        // 공식 제출용 문제 실행 환경 생성
        return judgeApplicationPort.createEnvironment(new CreateEnvironmentInput(
                new JudgeDatasetId(datasetId), new EnvironmentPolicy(true, true, false),
                QueuePriority.NORMAL, createQueueStatusListener(remainingTaskListener, detailListener)
        )).getValue();
    }

    @Override
    public ProblemJudgeExecutionResult executeInteractiveSql(String executionId, String environmentId,
                                                             String sql, int page, int pageSize) {
        // 인터랙티브 실행 옵션으로 SQL 실행
        return toProblemResult(judgeApplicationPort.executeSql(new ExecuteSqlInput(
                new JudgeExecutionId(executionId), new JudgeEnvironmentId(environmentId),
                sql, new ExecutionOptions(ExecutionOptions.DEFAULT_TIMEOUT_SECONDS, page, pageSize, true, false)
        )));
    }

    @Override
    public ProblemJudgeExecutionResult executePreviewSql(String executionId, String environmentId,
                                                         String sql, int page, int pageSize) {
        // 예시 생성 옵션으로 SQL 실행
        return toProblemResult(judgeApplicationPort.executeSql(new ExecuteSqlInput(
                new JudgeExecutionId(executionId), new JudgeEnvironmentId(environmentId),
                sql, ExecutionOptions.preview(page, pageSize)
        )));
    }

    @Override
    public ProblemJudgeExecutionResult executeInternalMetadataSql(String executionId, String environmentId,
                                                                  String sql, int pageSize) {
        // 내부 metadata 조회 옵션으로 SQL 실행
        return toProblemResult(judgeApplicationPort.executeSql(new ExecuteSqlInput(
                new JudgeExecutionId(executionId), new JudgeEnvironmentId(environmentId),
                sql, ExecutionOptions.internalMetadata(pageSize)
        )));
    }

    @Override
    public ProblemJudgeExecutionResult executeOfficialSql(String executionId, String environmentId, String sql) {
        // 공식 비용 측정 옵션으로 SQL 실행
        return toProblemResult(judgeApplicationPort.executeSql(new ExecuteSqlInput(
                new JudgeExecutionId(executionId), new JudgeEnvironmentId(environmentId),
                sql, ExecutionOptions.officialCost()
        )));
    }

    @Override
    public ProblemJudgeExecutionResult executeSubmissionAnswerSql(String executionId, String environmentId, String sql) {
        // 제출 정답 비교 옵션으로 SELECT 전체 결과 조회
        return toProblemResult(judgeApplicationPort.executeSelectAllSql(new ExecuteSqlInput(
                new JudgeExecutionId(executionId), new JudgeEnvironmentId(environmentId),
                sql, ExecutionOptions.submissionAnswer()
        )));
    }

    @Override
    public ProblemJudgeExecutionResult analyzeOfficialEnvironment(String executionId, String environmentId) {
        // 공식 비용 측정 옵션으로 실행 환경 통계 갱신
        return toProblemResult(judgeApplicationPort.analyzeEnvironment(new AnalyzeEnvironmentInput(
                new JudgeExecutionId(executionId), new JudgeEnvironmentId(environmentId), ExecutionOptions.officialCost()
        )));
    }

    @Override
    public void dropEnvironment(String environmentId) {
        // 실행 환경 제거
        judgeApplicationPort.dropEnvironment(new JudgeEnvironmentId(environmentId));
    }

    @Override
    public void cancelExecution(String executionId) {
        // 진행 중인 채점 실행 취소
        judgeApplicationPort.cancelExecution(new JudgeExecutionId(executionId));
    }

    @Override
    public List<ProblemSqlStatement> parseStatements(String sql) {
        // SQL 파서 결과를 problem 전용 문장 모델로 변환
        return judgeApplicationPort.parseSqlStatements(sql).stream()
                .map(statement -> new ProblemSqlStatement(statement.getSql(), toProblemMode(statement.getMode())))
                .toList();
    }

    private QueueStatusListener createQueueStatusListener(Consumer<Integer> remainingTaskListener,
                                                          Consumer<String> detailListener) {
        // runner queue와 LVM runtime 상세 이벤트를 problem 진행 상세 메시지로 변환
        Consumer<Integer> safeRemainingTaskListener = remainingTaskListener != null ? remainingTaskListener : ignored -> {
        };
        Consumer<String> safeDetailListener = detailListener != null ? detailListener : ignored -> {
        };

        return new QueueStatusListener() {

            @Override
            public void onWaiting(int remainingTasks) {
                // 대기열 잔여 작업 수 전달
                safeRemainingTaskListener.accept(remainingTasks);
            }

            @Override
            public void onSnapshotCreating() {
                // LVM snapshot 생성 시작 메시지 전달
                safeDetailListener.accept("LVM snapshot 생성 중");
            }

            @Override
            public void onSnapshotCreated() {
                // LVM snapshot 생성 완료 메시지 전달
                safeDetailListener.accept("LVM snapshot 생성 완료");
            }

            @Override
            public void onProcessStarting(DbmsType dbmsType) {
                // DBMS 프로세스 실행 시작 메시지 전달
                safeDetailListener.accept(dbmsType.getLabel() + " 프로세스 실행 중");
            }

            @Override
            public void onProcessStarted(DbmsType dbmsType) {
                // DBMS 프로세스 실행 완료 메시지 전달
                safeDetailListener.accept(dbmsType.getLabel() + " 프로세스 실행 완료");
            }
        };
    }

    private ProblemJudgeExecutionResult toProblemResult(SqlExecutionResult result) {
        // 채점 실행 결과를 problem 포트 출력 모델로 변환
        return new ProblemJudgeExecutionResult(
                toProblemMode(result.getMode()),
                result.getColumns(), result.getRows(),
                result.getRowCount(), result.getCurrentPage(), result.getPageSize(),
                result.getExecutionTimeMs(), result.getCost(), result.getPlanLines()
        );
    }

    private ProblemJudgeExecutionMode toProblemMode(ExecutionMode mode) {
        // 채점 실행 모드를 problem 전용 실행 모드로 변환
        return switch (mode) {
            case SELECT -> ProblemJudgeExecutionMode.SELECT;
            case EXPLAIN -> ProblemJudgeExecutionMode.EXPLAIN;
            case EXPLAIN_ANALYZE -> ProblemJudgeExecutionMode.EXPLAIN_ANALYZE;
            case ANALYZE -> ProblemJudgeExecutionMode.ANALYZE;
            case INDEX_COMMAND -> ProblemJudgeExecutionMode.INDEX_COMMAND;
            case COMMAND -> ProblemJudgeExecutionMode.COMMAND;
        };
    }
}
