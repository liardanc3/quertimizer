package com.quertimizer.judge.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqlStatementParser")
class SqlStatementParserTest {

    private final SqlStatementParser sqlStatementParser = new SqlStatementParser();

    @Nested
    @DisplayName("splitStatements")
    class SplitStatements {

        @Test
        @DisplayName("성공 (문자열 내부 세미콜론 유지)")
        void successWhenSemicolonInsideString() {
            // given
            String sql = "SELECT 'a;b'; SELECT 1;";

            // when
            var statements = sqlStatementParser.splitStatements(sql);

            // then
            assertThat(statements).containsExactly("SELECT 'a;b'", "SELECT 1");
        }

        @Test
        @DisplayName("성공 (주석 내부 세미콜론 무시)")
        void successWhenSemicolonInsideComment() {
            // given
            String sql = """
                    -- comment;
                    SELECT 1;
                    /* block; comment */
                    SELECT 2;
                    """;

            // when
            var statements = sqlStatementParser.splitStatements(sql);

            // then
            assertThat(statements).containsExactly("-- comment;\nSELECT 1", "/* block; comment */\nSELECT 2");
        }

        @Test
        @DisplayName("성공 (공백 문장 제거)")
        void successWhenBlankStatementsExist() {
            // given
            String sql = " ; SELECT 1; ; ";

            // when
            var statements = sqlStatementParser.splitStatements(sql);

            // then
            assertThat(statements).containsExactly("SELECT 1");
        }
    }
}
