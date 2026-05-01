package com.quertimizer.judge.application.port;

import com.quertimizer.judge.application.input.AnalyzeJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateJudgeDatasetInput;
import com.quertimizer.judge.application.input.CreateJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateJudgeReferenceInput;
import com.quertimizer.judge.application.input.CreateJudgeSetupSqlInput;
import com.quertimizer.judge.application.input.ExecuteIsolatedJudgeSqlInput;
import com.quertimizer.judge.application.input.ExecuteJudgeSqlInput;
import com.quertimizer.judge.application.output.JudgeSqlStatement;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.output.SqlReferenceResult;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import com.quertimizer.judge.domain.entity.ids.JudgeSetupSqlId;

import java.util.List;

public interface JudgePort {

    JudgeDatasetId createDataset(CreateJudgeDatasetInput input);

    JudgeSetupSqlId createSetupSql(CreateJudgeSetupSqlInput input);

    SqlReferenceResult createReference(CreateJudgeReferenceInput input);

    JudgeEnvironmentId createEnvironment(CreateJudgeEnvironmentInput input);

    SqlExecutionResult execute(ExecuteJudgeSqlInput input);

    SqlExecutionResult executeIsolated(ExecuteIsolatedJudgeSqlInput input);

    SqlExecutionResult analyze(AnalyzeJudgeEnvironmentInput input);

    List<JudgeSqlStatement> parseStatements(String sql);

    void cancel(JudgeExecutionId executionId);

    void drop(JudgeEnvironmentId environmentId);
}
