package com.quertimizer.problem.application.port.out;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.output.ProblemSqlStatement;

import java.util.List;
import java.util.function.Consumer;

public interface ProblemJudgePort {

    Long createDataset(DbmsType dbmsType, String ddl, String dataSql);

    Long createInlineDataset(DbmsType dbmsType, String ddl, String dataSql);

    Long createTemporaryDataset(DbmsType dbmsType, String ddl, String dataSql);

    boolean hasDataset(Long datasetId);

    void deleteDataset(Long datasetId);

    String createAnswerHash(Long datasetId, String answerSql);

    String createInteractiveEnvironment(Long datasetId);

    String createInteractiveEnvironment(Long datasetId, Consumer<Integer> remainingTaskListener);

    String createInteractiveEnvironment(Long datasetId, Consumer<Integer> remainingTaskListener,
                                        Consumer<String> detailListener);

    String createSubmissionEnvironment(Long datasetId);

    String createSubmissionEnvironment(Long datasetId, Consumer<Integer> remainingTaskListener);

    String createSubmissionEnvironment(Long datasetId, Consumer<Integer> remainingTaskListener,
                                       Consumer<String> detailListener);

    ProblemJudgeExecutionResult executeInteractiveSql(String executionId, String environmentId,
                                                      String sql, int page, int pageSize);

    ProblemJudgeExecutionResult executeInternalMetadataSql(String executionId, String environmentId,
                                                           String sql, int pageSize);

    ProblemJudgeExecutionResult executeOfficialSql(String executionId, String environmentId, String sql);

    ProblemJudgeExecutionResult executeSubmissionAnswerSql(String executionId, String environmentId, String sql);

    ProblemJudgeExecutionResult analyzeOfficialEnvironment(String executionId, String environmentId);

    void dropEnvironment(String environmentId);

    void cancelExecution(String executionId);

    List<ProblemSqlStatement> parseStatements(String sql);
}
