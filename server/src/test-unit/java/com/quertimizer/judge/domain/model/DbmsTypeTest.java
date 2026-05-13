package com.quertimizer.judge.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DbmsType")
class DbmsTypeTest {

    @Nested
    @DisplayName("fromValue")
    class FromValue {

        @Test
        @DisplayName("성공 (대소문자 무관 조회)")
        void successWhenValueCaseInsensitive() {
            // given
            String value = "PostgreSQL";

            // when
            var dbmsType = DbmsType.fromValue(value);

            // then
            assertThat(dbmsType).contains(DbmsType.POSTGRESQL);
        }
    }

    @Nested
    @DisplayName("isScopedProblemId")
    class IsScopedProblemId {

        @Test
        @DisplayName("성공 (스코프 문제 번호 여부 반환)")
        void successWhenScopedProblemIdChecked() {
            // given
            String problemId = "P00001-00001";

            // when
            boolean scopedProblemId = DbmsType.isScopedProblemId(problemId);

            // then
            assertThat(scopedProblemId).isTrue();
        }
    }

    @Nested
    @DisplayName("extractBaseProblemSetId")
    class ExtractBaseProblemSetId {

        @Test
        @DisplayName("성공 (스코프 값에서 기준 문제셋 번호 추출)")
        void successWhenScopedValue() {
            // given
            String scopedValue = "P00012-00003";

            // when
            String baseProblemSetId = DbmsType.extractBaseProblemSetId(scopedValue);

            // then
            assertThat(baseProblemSetId).isEqualTo("00012");
        }
    }
}
