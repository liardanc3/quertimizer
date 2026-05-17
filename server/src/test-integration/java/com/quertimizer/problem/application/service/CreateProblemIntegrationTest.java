package com.quertimizer.problem.application.service;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.input.ProblemCreateInput;
import com.quertimizer.problem.application.output.ProblemCreateOutput;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.port.in.CreateProblemUseCase;
import com.quertimizer.problem.application.port.out.ProblemAnswerCaseRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.application.port.out.ProblemRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSetHiddenCaseRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSetRepositoryPort;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemAnswerCase;
import com.quertimizer.problem.domain.entity.ProblemSet;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:create-problem-test;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@DisplayName("CreateProblem")
class CreateProblemIntegrationTest {

    @Autowired private CreateProblemUseCase createProblem;
    @Autowired private ProblemRepositoryPort problemRepository;
    @Autowired private ProblemSetRepositoryPort problemSetRepository;
    @Autowired private ProblemAnswerCaseRepositoryPort problemAnswerCaseRepository;
    @Autowired private ProblemSetHiddenCaseRepositoryPort problemSetHiddenCaseRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private ProblemJudgePort problemJudgePort;

    @BeforeEach
    void setUp() {
        reset(problemJudgePort);
        cleanProblemTables();
        enableIdentityColumn("problem_set", "id");
        enableIdentityColumn("problem", "id");
        enableIdentityColumn("problem_answer_case", "id");
        enableIdentityColumn("problem_set_hidden_case", "id");
    }

    @Nested
    @DisplayName("POST /admin/problems")
    class Execute {

        @Test
        @DisplayName("성공 (신규 테이블셋과 문제 생성)")
        void successWhenCreateNewProblemSetAndProblem() {
            // given
            ProblemCreateInput input = createInput("", "");
            when(problemJudgePort.createDataset(input.getDbmsType(), input.getDdl(), input.getActualDataSql())).thenReturn(1001L);
            when(problemJudgePort.createInlineDataset(input.getDbmsType(), input.getDdl(), input.getHiddenDataSqls().get(0))).thenReturn(2001L);
            when(problemJudgePort.createInlineDataset(input.getDbmsType(), input.getDdl(), input.getHiddenDataSqls().get(1))).thenReturn(2002L);
            when(problemJudgePort.createAnswerHash(eq(1001L), eq(input.getAnswerSql()))).thenReturn("hash-open");
            when(problemJudgePort.createAnswerHash(eq(2001L), eq(input.getAnswerSql()))).thenReturn("hash-hidden-1");
            when(problemJudgePort.createAnswerHash(eq(2002L), eq(input.getAnswerSql()))).thenReturn("hash-hidden-2");
            when(problemJudgePort.createSubmissionEnvironment(1001L)).thenReturn("env-1001");
            when(problemJudgePort.executeInternalMetadataSql(anyString(), eq("env-1001"), anyString(), anyInt())).thenReturn(emptyResult());
            when(problemJudgePort.executeInteractiveSql(anyString(), eq("env-1001"), anyString(), anyInt(), anyInt()))
                    .thenReturn(selectResult());

            // when
            ProblemCreateOutput output = createProblem.execute(input);

            // then
            Problem problem = problemRepository.findByProblemId(output.problemId()).orElseThrow();
            ProblemSet problemSet = problemSetRepository.findByProblemSetId(problem.getProblemSetId()).orElseThrow();
            assertThat(problem.getTitle()).isEqualTo("첫 번째 VIP");
            assertThat(problem.getAnswerHash()).isEqualTo("hash-open");
            assertThat(problemSet.getDatasetId()).isEqualTo(1001L);
            assertThat(problemSetHiddenCaseRepository.findAllByProblemSetIdOrderByCaseOrderAsc(problemSet.getProblemSetId()))
                    .extracting("datasetId")
                    .containsExactly(2001L, 2002L);
            assertThat(problemAnswerCaseRepository.findActualByProblemId(problem.getProblemId()))
                    .map(ProblemAnswerCase::getAnswerHash)
                    .contains("hash-open");
            assertThat(problemAnswerCaseRepository.findHiddenByProblemIdOrderByCaseOrderAsc(problem.getProblemId()))
                    .extracting(ProblemAnswerCase::getAnswerHash)
                    .containsExactly("hash-hidden-1", "hash-hidden-2");
            verify(problemJudgePort).dropEnvironment("env-1001");
        }

        @Test
        @DisplayName("성공 (기존 문제는 설명 정보만 수정)")
        void successWhenUpdateExistingProblemTextOnly() {
            // given
            ProblemSet problemSet = problemSetRepository.save(ProblemSet.create(
                    "P77777", ddl(), actualDataSql(), DbmsType.POSTGRESQL, 777L
            ));
            Problem storedProblem = problemRepository.save(Problem.create(
                    "P77777-00001", problemSet.getProblemSetId(),
                    "기존 제목", "기존 설명", ddl(), DbmsType.POSTGRESQL,
                    "기존 조건", "기존 출력", "[]", "[]", "{}", "hash-old", answerSql()
            ));
            ProblemCreateInput input = createInput(problemSet.getProblemSetId(), storedProblem.getProblemId());

            // when
            ProblemCreateOutput output = createProblem.execute(input);

            // then
            Problem problem = problemRepository.findByProblemId(output.problemId()).orElseThrow();
            assertThat(problem.getProblemId()).isEqualTo(storedProblem.getProblemId());
            assertThat(problem.getTitle()).isEqualTo("첫 번째 VIP");
            assertThat(problem.getDescription()).isEqualTo("VIP 고객 중 가장 오래전에 가입한 고객의 이메일을 출력한다.");
            assertThat(problem.getAnswerHash()).isEqualTo("hash-old");
            verifyNoInteractions(problemJudgePort);
        }

        @Test
        @DisplayName("Throw (정답 해시 생성 실패 시 생성 데이터셋 정리)")
        void throwWhenAnswerHashCreationFailsThenCreatedDatasetIsDeleted() {
            // given
            ProblemCreateInput input = createInput("", "");
            when(problemJudgePort.createDataset(input.getDbmsType(), input.getDdl(), input.getActualDataSql())).thenReturn(9001L);
            when(problemJudgePort.createAnswerHash(9001L, input.getAnswerSql())).thenThrow(new IllegalStateException("정답 기준 생성 실패"));

            // when
            Throwable thrown = catchThrowable(() -> createProblem.execute(input));

            // then
            assertThat(thrown).isInstanceOf(BusinessException.class);
            verify(problemJudgePort).deleteDataset(9001L);
        }
    }

    private ProblemCreateInput createInput(String problemSetId, String problemId) {
        return new ProblemCreateInput(
                "첫 번째 VIP", "VIP 고객 중 가장 오래전에 가입한 고객의 이메일을 출력한다.",
                "가입일이 같으면 customer_id가 가장 낮은 고객을 출력한다.", "email(이메일)",
                answerSql(), actualDataSql(), problemSetId, problemId, "postgresql", ddl(), ddl(),
                List.of(hiddenDataSql1(), hiddenDataSql2())
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
        jdbcTemplate.update("DELETE FROM problem_answer_case");
        jdbcTemplate.update("DELETE FROM problem_set_hidden_case");
        jdbcTemplate.update("DELETE FROM problem");
        jdbcTemplate.update("DELETE FROM problem_set");
    }

    private String ddl() {
        return """
                CREATE TABLE customers (
                    customer_id BIGINT PRIMARY KEY,
                    email VARCHAR(120) NOT NULL,
                    grade VARCHAR(20) NOT NULL,
                    signup_date DATE NOT NULL
                );
                """;
    }

    private String actualDataSql() {
        return """
                INSERT INTO customers (customer_id, email, grade, signup_date) VALUES
                    (1, 'vip1@quertimizer.com', 'VIP', DATE '2020-01-01'),
                    (2, 'basic1@quertimizer.com', 'BASIC', DATE '2019-01-01');
                """;
    }

    private String hiddenDataSql1() {
        return """
                INSERT INTO customers (customer_id, email, grade, signup_date) VALUES
                    (10, 'hidden1@quertimizer.com', 'VIP', DATE '2018-01-01');
                """;
    }

    private String hiddenDataSql2() {
        return """
                INSERT INTO customers (customer_id, email, grade, signup_date) VALUES
                    (20, 'hidden2@quertimizer.com', 'VIP', DATE '2017-01-01');
                """;
    }

    private String answerSql() {
        return """
                SELECT email
                FROM customers
                WHERE grade = 'VIP'
                ORDER BY signup_date ASC, customer_id ASC
                LIMIT 1
                """;
    }

    private ProblemJudgeExecutionResult emptyResult() {
        return new ProblemJudgeExecutionResult(
                ProblemJudgeExecutionMode.SELECT, List.of(), List.of(),
                0, 1, 10, 1L, BigDecimal.ZERO, List.of()
        );
    }

    private ProblemJudgeExecutionResult selectResult() {
        return new ProblemJudgeExecutionResult(
                ProblemJudgeExecutionMode.SELECT, List.of("email"), List.of(List.of("vip1@quertimizer.com")),
                1, 1, 10, 1L, BigDecimal.ONE, List.of()
        );
    }
}
