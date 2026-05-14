package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.application.input.AnalyzeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateDatasetInput;
import com.quertimizer.judge.application.input.CreateEnvironmentInput;
import com.quertimizer.judge.application.input.CreateSqlExecutionHashInput;
import com.quertimizer.judge.application.input.ExecuteSqlInput;
import com.quertimizer.judge.application.output.SqlExecutionHashResult;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.output.SqlStatement;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.model.DatabaseSnapshot;

import java.util.List;

public interface JudgeApplicationPort {

    JudgeDatasetId createDataset(CreateDatasetInput input);

    boolean hasDataset(Long datasetId);

    void deleteDataset(JudgeDatasetId datasetId);

    JudgeEnvironmentId createEnvironment(CreateEnvironmentInput input);

    void dropEnvironment(JudgeEnvironmentId environmentId);

    SqlExecutionResult executeSql(ExecuteSqlInput input);

    SqlExecutionResult analyzeEnvironment(AnalyzeEnvironmentInput input);

    SqlExecutionHashResult createSqlExecutionHash(CreateSqlExecutionHashInput input);

    void cancelExecution(JudgeExecutionId executionId);

    List<SqlStatement> parseSqlStatements(String sql);

    DatabaseSnapshot createDatabaseSnapshot();
}
