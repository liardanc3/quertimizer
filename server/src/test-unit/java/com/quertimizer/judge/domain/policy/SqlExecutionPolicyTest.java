package com.quertimizer.judge.domain.policy;

import com.quertimizer.judge.domain.model.ExecutionMode;
import com.quertimizer.judge.domain.service.SqlStatementParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SqlExecutionPolicy")
class SqlExecutionPolicyTest {

    private final SqlExecutionPolicy sqlExecutionPolicy = new SqlExecutionPolicy(new SqlStatementParser());

    @Nested
    @DisplayName("resolveMode")
    class ResolveMode {

        @Test
        @DisplayName("성공 (SELECT 모드)")
        void successWhenSelectSql() {
            // given
            String sql = "select * from customers";

            // when
            ExecutionMode mode = sqlExecutionPolicy.resolveMode(sql);

            // then
            assertThat(mode).isEqualTo(ExecutionMode.SELECT);
        }

        @Test
        @DisplayName("성공 (EXPLAIN ANALYZE 모드)")
        void successWhenExplainAnalyzeSql() {
            // given
            String sql = "EXPLAIN ANALYZE SELECT * FROM customers";

            // when
            ExecutionMode mode = sqlExecutionPolicy.resolveMode(sql);

            // then
            assertThat(mode).isEqualTo(ExecutionMode.EXPLAIN_ANALYZE);
        }

        @Test
        @DisplayName("성공 (인덱스 명령 모드)")
        void successWhenIndexCommandSql() {
            // given
            String sql = "CREATE INDEX idx_customers_email ON customers(email)";

            // when
            ExecutionMode mode = sqlExecutionPolicy.resolveMode(sql);

            // then
            assertThat(mode).isEqualTo(ExecutionMode.INDEX_COMMAND);
        }

        @Test
        @DisplayName("실패 (복수 SQL 문장)")
        void failWhenMultipleStatements() {
            // given
            String sql = "SELECT 1; SELECT 2";

            // when & then
            assertThatThrownBy(() -> sqlExecutionPolicy.resolveMode(sql))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("하나만");
        }

        @Test
        @DisplayName("실패 (쓰기 CTE)")
        void failWhenWritableCte() {
            // given
            String sql = "WITH changed AS (DELETE FROM customers RETURNING *) SELECT * FROM changed";

            // when & then
            assertThatThrownBy(() -> sqlExecutionPolicy.resolveMode(sql))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
