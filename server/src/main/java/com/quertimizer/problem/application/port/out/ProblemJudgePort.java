package com.quertimizer.problem.application.port.out;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.output.ProblemJudgeSqlStatement;

import java.util.List;
import java.util.function.Consumer;

public interface ProblemJudgePort {

    String createDataset(DbmsType dbmsType, String ddl, String dataSql);

    String createInlineDataset(DbmsType dbmsType, String ddl, String dataSql);

    String createTemporaryDataset(DbmsType dbmsType, String ddl, String dataSql);

    boolean hasDataset(String datasetId);

    void deleteDataset(String datasetId);

    String createAnswerHash(String datasetId, String answerSql);

    String createInteractiveEnvironment(String datasetId);

    String createInteractiveEnvironment(String datasetId, Consumer<Integer> remainingTaskListener);

    String createSubmissionEnvironment(String datasetId);

    String createSubmissionEnvironment(String datasetId, Consumer<Integer> remainingTaskListener);

    String createSubmissionEnvironment(String datasetId, Consumer<Integer> remainingTaskListener,
                                       Consumer<String> detailListener);

    ProblemJudgeExecutionResult executeInteractiveSql(String executionId, String environmentId,
                                                      String sql, int page, int pageSize);

    ProblemJudgeExecutionResult executeInternalMetadataSql(String executionId, String environmentId,
                                                           String sql, int pageSize);

    ProblemJudgeExecutionResult executeOfficialSql(String executionId, String environmentId, String sql);

    ProblemJudgeExecutionResult executeSubmissionAnswerSql(String executionId, String environmentId, String sql);

    ProblemJudgeExecutionResult analyzeOfficialEnvironment(String executionId, String environmentId);

    ProblemJudgeExecutionResult executeIsolatedOfficialSql(String executionId, String datasetId, String sql);

    ProblemJudgeExecutionResult executeIsolatedSubmissionAnswerSql(String executionId, String datasetId, String sql);

    void dropEnvironment(String environmentId);

    void cancelExecution(String executionId);

    List<ProblemJudgeSqlStatement> parseStatements(String sql);
}
