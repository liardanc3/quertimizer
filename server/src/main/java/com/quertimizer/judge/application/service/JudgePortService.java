package com.quertimizer.judge.application.service;

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
import com.quertimizer.judge.application.port.JudgePort;
import com.quertimizer.judge.application.usecase.AnalyzeJudgeEnvironment;
import com.quertimizer.judge.application.usecase.CancelJudgeExecution;
import com.quertimizer.judge.application.usecase.CreateJudgeDataset;
import com.quertimizer.judge.application.usecase.CreateJudgeEnvironment;
import com.quertimizer.judge.application.usecase.CreateJudgeReference;
import com.quertimizer.judge.application.usecase.CreateJudgeSetupSql;
import com.quertimizer.judge.application.usecase.DropJudgeEnvironment;
import com.quertimizer.judge.application.usecase.ExecuteIsolatedJudgeSql;
import com.quertimizer.judge.application.usecase.ExecuteJudgeSql;
import com.quertimizer.judge.application.usecase.ParseJudgeSqlStatements;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import com.quertimizer.judge.domain.entity.ids.JudgeSetupSqlId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JudgePortService implements JudgePort {

    private final CreateJudgeDataset createJudgeDataset;
    private final CreateJudgeSetupSql createJudgeSetupSql;
    private final CreateJudgeReference createJudgeReference;
    private final CreateJudgeEnvironment createJudgeEnvironment;
    private final ExecuteJudgeSql executeJudgeSql;
    private final ExecuteIsolatedJudgeSql executeIsolatedJudgeSql;
    private final AnalyzeJudgeEnvironment analyzeJudgeEnvironment;
    private final ParseJudgeSqlStatements parseJudgeSqlStatements;
    private final CancelJudgeExecution cancelJudgeExecution;
    private final DropJudgeEnvironment dropJudgeEnvironment;

    @Override
    public JudgeDatasetId createDataset(CreateJudgeDatasetInput input) {
        return createJudgeDataset.execute(input);
    }

    @Override
    public JudgeSetupSqlId createSetupSql(CreateJudgeSetupSqlInput input) {
        return createJudgeSetupSql.execute(input);
    }

    @Override
    public SqlReferenceResult createReference(CreateJudgeReferenceInput input) {
        return createJudgeReference.execute(input);
    }

    @Override
    public JudgeEnvironmentId createEnvironment(CreateJudgeEnvironmentInput input) {
        return createJudgeEnvironment.execute(input);
    }

    @Override
    public SqlExecutionResult execute(ExecuteJudgeSqlInput input) {
        return executeJudgeSql.execute(input);
    }

    @Override
    public SqlExecutionResult executeIsolated(ExecuteIsolatedJudgeSqlInput input) {
        return executeIsolatedJudgeSql.execute(input);
    }

    @Override
    public SqlExecutionResult analyze(AnalyzeJudgeEnvironmentInput input) {
        return analyzeJudgeEnvironment.execute(input);
    }

    @Override
    public List<JudgeSqlStatement> parseStatements(String sql) {
        return parseJudgeSqlStatements.execute(sql);
    }

    @Override
    public void cancel(JudgeExecutionId executionId) {
        cancelJudgeExecution.execute(executionId);
    }

    @Override
    public void drop(JudgeEnvironmentId environmentId) {
        dropJudgeEnvironment.execute(environmentId);
    }
}
