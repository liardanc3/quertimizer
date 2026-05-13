package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.application.input.ExecuteSqlInput;
import com.quertimizer.judge.application.model.EnvironmentConnection;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.entity.SetupSqlDefinition;
import com.quertimizer.judge.domain.model.ExecutionMode;

import java.util.List;

public interface SqlExecutionPort {

    SqlExecutionResult execute(ExecuteSqlInput command, String sql, ExecutionMode mode,
                               EnvironmentConnection environmentConnection) throws Exception;

    void executeSetupSqls(EnvironmentConnection environmentConnection,
                          List<SetupSqlDefinition> setupSqlDefinitions) throws Exception;

    SqlExecutionResult executeSelectAll(JudgeExecutionId executionId,
                                        EnvironmentConnection environmentConnection,
                                        String sql, boolean includeCost,
                                        boolean includePlan) throws Exception;

    SqlExecutionResult executeAnalyze(JudgeExecutionId executionId,
                                      EnvironmentConnection environmentConnection) throws Exception;

    boolean hasActiveExecution(JudgeExecutionId executionId);

    void cancel(JudgeExecutionId executionId);
}
