package com.quertimizer.problem.application.service;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.output.ProblemSqlStatement;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ExecuteProblemSql")
class ExecuteProblemSqlTest {

    private final ProblemJudgePort problemJudgePort = mock(ProblemJudgePort.class);
    private final ProblemDatasetResolver datasetResolver = mock(ProblemDatasetResolver.class);
    private final ProblemExecutionSessionStore executionSessionStore = new ProblemExecutionSessionStore();
    private final ExecuteProblemSql executeProblemSql =
            new ExecuteProblemSql(problemJudgePort, datasetResolver, executionSessionStore);

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("성공 (인덱스 없는 실행은 통계 갱신 생략)")
        void successWhenIndexSqlMissingSkipsAnalyze() {
            // given
            ProblemExecutionInput input = input(List.of());
            givenExecutionReady(input, "SELECT 1");

            // when
            executeProblemSql.execute(input);

            // then
            verify(problemJudgePort, never()).analyzeOfficialEnvironment(any(), any());
        }

        @Test
        @DisplayName("성공 (인덱스 있는 실행은 통계 갱신)")
        void successWhenIndexSqlExistsAnalyze() {
            // given
            ProblemExecutionInput input = input(List.of("CREATE INDEX index_01 ON customers(customer_id)"));
            givenExecutionReady(input, "SELECT 1");

            // when
            executeProblemSql.execute(input);

            // then
            verify(problemJudgePort).analyzeOfficialEnvironment(any(), eq("env-1"));
        }
    }

    private ProblemExecutionInput input(List<String> indexSqls) {
        return ProblemExecutionInput.of(
                "tester", "session-1", "P00001-00001",
                "SELECT 1", "POSTGRESQL", 1, 10, indexSqls
        );
    }

    private void givenExecutionReady(ProblemExecutionInput input, String sql) {
        ProblemDatasetResolver.ResolvedProblemDataset dataset = mock(ProblemDatasetResolver.ResolvedProblemDataset.class);
        when(dataset.getDatasetId()).thenReturn(1L);
        when(dataset.getDbmsType()).thenReturn(DbmsType.POSTGRESQL);
        when(datasetResolver.resolve(input.getProblemId(), input.getDbmsType())).thenReturn(dataset);
        when(problemJudgePort.parseStatements(input.getSql()))
                .thenReturn(List.of(new ProblemSqlStatement(sql, ProblemJudgeExecutionMode.SELECT)));
        when(problemJudgePort.createInteractiveEnvironment(eq(1L), any(), any())).thenReturn("env-1");
        when(problemJudgePort.executeInteractiveSql(any(), eq("env-1"), any(), anyInt(), anyInt()))
                .thenReturn(selectResult());
    }

    private ProblemJudgeExecutionResult selectResult() {
        return new ProblemJudgeExecutionResult(
                ProblemJudgeExecutionMode.SELECT,
                List.of("value"), List.of(List.of("1")),
                1, 1, 10, 1L, BigDecimal.ONE, List.of()
        );
    }
}
