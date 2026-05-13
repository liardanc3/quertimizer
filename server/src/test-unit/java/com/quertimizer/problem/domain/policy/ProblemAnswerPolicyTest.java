package com.quertimizer.problem.domain.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemAnswerPolicy")
class ProblemAnswerPolicyTest {

    private final ProblemAnswerPolicy problemAnswerPolicy = new ProblemAnswerPolicy();

    @Nested
    @DisplayName("matches")
    class Matches {

        @Test
        @DisplayName("성공 (canonical 결과 해시 일치)")
        void successWhenCanonicalHashMatches() {
            // given
            List<String> columns = List.of("email", "total");
            List<List<String>> rows = List.of(List.of("a@quertimizer.com", "10"));
            String answerHash = ProblemAnswerHashSupport.hashResult(columns, rows);

            // when
            boolean matched = problemAnswerPolicy.matches(answerHash, columns, rows);

            // then
            assertThat(matched).isTrue();
        }

        @Test
        @DisplayName("성공 (legacy 행 해시 일치)")
        void successWhenLegacyHashMatches() {
            // given
            List<String> columns = List.of("email", "total");
            List<List<String>> rows = List.of(List.of("a@quertimizer.com", "10"));
            String answerHash = ProblemAnswerHashSupport.hashRows(rows);

            // when
            boolean matched = problemAnswerPolicy.matches(answerHash, columns, rows);

            // then
            assertThat(matched).isTrue();
        }

        @Test
        @DisplayName("실패 (빈 정답 해시)")
        void failWhenAnswerHashBlank() {
            // given
            List<String> columns = List.of("email");
            List<List<String>> rows = List.of(List.of("a@quertimizer.com"));

            // when
            boolean matched = problemAnswerPolicy.matches("", columns, rows);

            // then
            assertThat(matched).isFalse();
        }
    }
}
