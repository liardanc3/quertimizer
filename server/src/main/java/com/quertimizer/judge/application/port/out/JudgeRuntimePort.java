package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.application.input.AnalyzeJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateDataset;
import com.quertimizer.judge.application.input.CreateJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateJudgeSetupSqlInput;
import com.quertimizer.judge.application.input.CreateSqlExecutionHashInput;
import com.quertimizer.judge.application.input.ExecuteJudgeSqlInput;
import com.quertimizer.judge.application.input.ExecuteIsolatedJudgeSqlInput;
import com.quertimizer.judge.domain.model.event.JudgeListener;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.entity.JudgeSetupSqlId;
import com.quertimizer.judge.application.output.SqlExecutionHashResult;
import com.quertimizer.judge.application.output.SqlExecutionResult;

import java.util.concurrent.CompletionStage;

public interface JudgeRuntimePort {

    JudgeDatasetId createDataset(CreateDataset command);

    void deleteDataset(JudgeDatasetId datasetId);

    JudgeSetupSqlId createSetupSql(CreateJudgeSetupSqlInput command);

    SqlExecutionHashResult createHash(CreateSqlExecutionHashInput command);

    JudgeEnvironmentId create(CreateJudgeEnvironmentInput command);

    JudgeExecutionId executeAsync(ExecuteJudgeSqlInput command, JudgeListener listener);

    CompletionStage<SqlExecutionResult> executeAsync(ExecuteJudgeSqlInput command);

    SqlExecutionResult execute(ExecuteJudgeSqlInput command);

    JudgeExecutionId executeIsolatedAsync(ExecuteIsolatedJudgeSqlInput command, JudgeListener listener);

    CompletionStage<SqlExecutionResult> executeIsolatedAsync(ExecuteIsolatedJudgeSqlInput command);

    SqlExecutionResult executeIsolated(ExecuteIsolatedJudgeSqlInput command);

    SqlExecutionResult analyze(AnalyzeJudgeEnvironmentInput command);

    void cancel(JudgeExecutionId executionId);

    void drop(JudgeEnvironmentId environmentId);
}
