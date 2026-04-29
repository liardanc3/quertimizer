package com.quertimizer.problem.infrastructure.sqljudge;

import com.quertimizer.problem.application.input.ProblemSqlDatasetInput;
import com.quertimizer.problem.application.input.ProblemSqlExecutionInput;
import com.quertimizer.problem.application.input.ProblemSqlReferenceInput;
import com.quertimizer.problem.application.output.ProblemSqlDatasetOutput;
import com.quertimizer.problem.application.output.ProblemSqlExecutionOutput;
import com.quertimizer.problem.application.output.ProblemSqlReferenceOutput;
import com.quertimizer.problem.application.port.ProblemSqlJudgePort;
import com.quertimizer.sqljudge.api.SqlJudge;
import com.quertimizer.sqljudge.command.CreateDatasetCommand;
import com.quertimizer.sqljudge.command.CreateReferenceCommand;
import com.quertimizer.sqljudge.command.IsolatedExecuteCommand;
import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeExecutionId;
import com.quertimizer.sqljudge.policy.ExecutionOptions;
import com.quertimizer.sqljudge.policy.IsolationPolicy;
import com.quertimizer.sqljudge.result.SqlExecutionResult;
import com.quertimizer.sqljudge.result.SqlReferenceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Adapts problem use cases to the sql-judge API.
 */
@Component
@RequiredArgsConstructor
public class ProblemSqlJudgeAdapter implements ProblemSqlJudgePort {

    private final SqlJudge sqlJudge;

    /**
     * Creates a problem SQL dataset through sql-judge.
     *
     * @param input dataset creation input
     * @return dataset creation result
     */
    @Override
    public ProblemSqlDatasetOutput createDataset(ProblemSqlDatasetInput input) {
        JudgeDatasetId datasetId = sqlJudge.createDataset(new CreateDatasetCommand(
                toSqlJudgeDbmsType(input.getDbmsType()),
                input.getDdl(),
                input.getDataSql(),
                input.getBaseIndexDdls()
        ));

        return new ProblemSqlDatasetOutput(datasetId.getValue());
    }

    /**
     * Executes SQL through sql-judge.
     *
     * @param input SQL execution input
     * @return SQL execution result
     */
    @Override
    public ProblemSqlExecutionOutput execute(ProblemSqlExecutionInput input) {
        SqlExecutionResult result = sqlJudge.executeIsolated(new IsolatedExecuteCommand(
                new JudgeExecutionId("problem-create-" + UUID.randomUUID()),
                new JudgeDatasetId(input.getDatasetId()),
                List.of(),
                input.getSql(),
                IsolationPolicy.cleanRoom(),
                ExecutionOptions.officialCost()
        ));

        return new ProblemSqlExecutionOutput(result.getColumns(), result.getRows(), result.getRowCount());
    }

    /**
     * Creates a reference SQL definition through sql-judge.
     *
     * @param input reference SQL creation input
     * @return reference SQL creation result
     */
    @Override
    public ProblemSqlReferenceOutput createReference(ProblemSqlReferenceInput input) {
        SqlReferenceResult result = sqlJudge.createReference(new CreateReferenceCommand(
                new JudgeDatasetId(input.getDatasetId()),
                input.getReferenceSql(),
                ExecutionOptions.officialCost()
        ));

        return new ProblemSqlReferenceOutput(result.getReferenceId().getValue(), result.getResultHash());
    }

    private com.quertimizer.sqljudge.db.DbmsType toSqlJudgeDbmsType(com.quertimizer.global.constant.DbmsType dbmsType) {
        return switch (dbmsType) {
            case POSTGRESQL -> com.quertimizer.sqljudge.db.DbmsType.POSTGRESQL;
            case MYSQL -> com.quertimizer.sqljudge.db.DbmsType.MYSQL;
        };
    }
}
