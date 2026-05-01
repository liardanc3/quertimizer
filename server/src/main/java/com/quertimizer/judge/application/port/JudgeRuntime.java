package com.quertimizer.judge.application.port;

import com.quertimizer.judge.application.input.AnalyzeJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateJudgeDatasetInput;
import com.quertimizer.judge.application.input.CreateJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateJudgeReferenceInput;
import com.quertimizer.judge.application.input.CreateJudgeSetupSqlInput;
import com.quertimizer.judge.application.input.ExecuteJudgeSqlInput;
import com.quertimizer.judge.application.input.ExecuteIsolatedJudgeSqlInput;
import com.quertimizer.judge.domain.event.JudgeListener;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import com.quertimizer.judge.domain.entity.ids.JudgeSetupSqlId;
import com.quertimizer.judge.application.output.SqlReferenceResult;
import com.quertimizer.judge.application.output.SqlExecutionResult;

import java.util.concurrent.CompletionStage;

public interface JudgeRuntime {

    JudgeDatasetId createDataset(CreateJudgeDatasetInput command);

    JudgeSetupSqlId createSetupSql(CreateJudgeSetupSqlInput command);

    SqlReferenceResult createReference(CreateJudgeReferenceInput command);

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
