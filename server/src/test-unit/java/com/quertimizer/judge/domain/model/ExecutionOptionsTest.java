package com.quertimizer.judge.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutionOptions")
class ExecutionOptionsTest {

    @Nested
    @DisplayName("submissionAnswer")
    class SubmissionAnswer {

        @Test
        @DisplayName("성공 (정답 검증 Cost 제외)")
        void successWhenCostExcluded() {
            // when
            ExecutionOptions options = ExecutionOptions.submissionAnswer();

            // then
            assertThat(options.isIncludeCost()).isFalse();
            assertThat(options.isIncludePlan()).isFalse();
        }
    }
}
