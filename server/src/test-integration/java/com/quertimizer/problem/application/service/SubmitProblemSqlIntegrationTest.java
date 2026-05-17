package com.quertimizer.problem.application.service;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.input.ProblemSubmissionInput;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.output.ProblemSqlStatement;
import com.quertimizer.problem.application.output.ProblemSubmissionOutput;
import com.quertimizer.problem.application.output.ProblemSubmissionProgress;
import com.quertimizer.problem.application.port.in.SubmitProblemSqlUseCase;
import com.quertimizer.problem.application.port.out.ProblemAnswerCaseRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.application.port.out.ProblemRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSetRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSolveHistoryRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemAnswerCase;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.problem.domain.entity.ids.ProblemSolveHistoryId;
import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:submit-problem-test;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@DisplayName("SubmitProblemSql")
class SubmitProblemSqlIntegrationTest {

    @Autowired private SubmitProblemSqlUseCase submitProblemSql;
    @Autowired private ProblemRepositoryPort problemRepository;
    @Autowired private ProblemSetRepositoryPort problemSetRepository;
    @Autowired private ProblemAnswerCaseRepositoryPort problemAnswerCaseRepository;
    @Autowired private ProblemSubmitHistoryRepositoryPort problemSubmitHistoryRepository;
    @Autowired private ProblemSolveHistoryRepositoryPort problemSolveHistoryRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private ProblemJudgePort problemJudgePort;

    @BeforeEach
    void setUp() {
        reset(problemJudgePort);
        cleanProblemTables();
        enableIdentityColumn("problem_set", "id");
        enableIdentityColumn("problem", "id");
        enableIdentityColumn("problem_answer_case", "id");
        enableIdentityColumn("problem_submit_history", "submit_id");
    }

    @Nested
    @DisplayName("MESSAGE problem.submit")
    class Execute {

        @Test
        @DisplayName("성공 (오답 제출은 Cost와 최고 기록을 저장하지 않음)")
        void successWhenIncorrectAnswerDoesNotStoreCostAndBestSolveHistory() {
            // given
            String problemId = "P88888-00001";
            String handle = "submit-tester";
            problemSetRepository.save(ProblemSet.create("P88888", ddl(), dataSql(), DbmsType.POSTGRESQL, 3301L));
            problemRepository.save(Problem.create(
                    problemId, "P88888",
                    "첫 번째 VIP", "설명", ddl(), DbmsType.POSTGRESQL,
                    "조건", "email(이메일)", "[]", "[]", "{}", "not-matching-hash", answerSql()
            ));
            problemAnswerCaseRepository.saveAll(List.of(ProblemAnswerCase.actual(problemId, 3301L, "not-matching-hash")));
            List<ProblemSubmissionProgress> progresses = new ArrayList<>();
            ProblemSubmissionInput input = ProblemSubmissionInput.of(
                    handle, problemId, answerSql(), "postgresql", progresses::add
            );
            when(problemJudgePort.hasDataset(3301L)).thenReturn(true);
            when(problemJudgePort.parseStatements(answerSql())).thenReturn(List.of(
                    new ProblemSqlStatement(answerSql(), ProblemJudgeExecutionMode.SELECT)
            ));
            when(problemJudgePort.createSubmissionEnvironment(
                    eq(3301L), org.mockito.ArgumentMatchers.<Consumer<Integer>>any(),
                    org.mockito.ArgumentMatchers.<Consumer<String>>any()
            )).thenAnswer(invocation -> {
                Consumer<Integer> remainingTaskListener = invocation.getArgument(1);
                Consumer<String> detailListener = invocation.getArgument(2);
                remainingTaskListener.accept(0);
                detailListener.accept("PostgreSQL 프로세스 실행 완료");
                return "submit-env-3301";
            });
            when(problemJudgePort.executeSubmissionAnswerSql(anyString(), eq("submit-env-3301"), eq(answerSql())))
                    .thenReturn(wrongAnswerResult());

            // when
            ProblemSubmissionOutput output = submitProblemSql.execute(input);

            // then
            List<ProblemSubmitHistory> histories = problemSubmitHistoryRepository.findAllByHandleOrderBySubmittedAtDesc(handle);
            assertThat(output.isSuccess()).isFalse();
            assertThat(output.getMessage()).isEqualTo("오답");
            assertThat(histories).hasSize(1);
            assertThat(histories.get(0).isSuccess()).isFalse();
            assertThat(histories.get(0).getCost()).isZero();
            assertThat(histories.get(0).getExecutionPlanElement()).isZero();
            assertThat(problemSolveHistoryRepository.findById(new ProblemSolveHistoryId(problemId, handle))).isEmpty();
            assertThat(progresses).extracting(ProblemSubmissionProgress::getStatus).contains("running", "incorrect");
            verify(problemJudgePort, never()).analyzeOfficialEnvironment(anyString(), anyString());
            verify(problemJudgePort).dropEnvironment("submit-env-3301");
        }
    }

    private String ddl() {
        return """
                CREATE TABLE customers (
                    customer_id BIGINT PRIMARY KEY,
                    email VARCHAR(120) NOT NULL
                );
                """;
    }

    private String dataSql() {
        return """
                INSERT INTO customers (customer_id, email) VALUES
                    (1, 'vip@quertimizer.com');
                """;
    }

    private String answerSql() {
        return "SELECT email FROM customers";
    }

    private ProblemJudgeExecutionResult wrongAnswerResult() {
        return new ProblemJudgeExecutionResult(
                ProblemJudgeExecutionMode.SELECT, List.of("email"), List.of(List.of("wrong@quertimizer.com")),
                1, 1, 10, 7L, BigDecimal.TEN, List.of()
        );
    }

    private void enableIdentityColumn(String tableName, String columnName) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " BIGINT GENERATED BY DEFAULT AS IDENTITY");
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " RESTART WITH 1");
        } catch (Exception exception) {
            throw new IllegalStateException("identity column setup failed: " + tableName + "." + columnName, exception);
        }
    }

    private void cleanProblemTables() {
        jdbcTemplate.update("DELETE FROM problem_submit_history");
        jdbcTemplate.update("DELETE FROM problem_answer_case");
        jdbcTemplate.update("DELETE FROM problem");
        jdbcTemplate.update("DELETE FROM problem_set");
    }
}
