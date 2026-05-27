package com.quertimizer.problem.domain.policy;

import com.quertimizer.problem.domain.model.ProblemPlanMeasurement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemOfficialCostPolicy")
class ProblemOfficialCostPolicyTest {

    private final ProblemOfficialCostPolicy problemOfficialCostPolicy = new ProblemOfficialCostPolicy();

    @Nested
    @DisplayName("getMeasurementAttemptCount")
    class GetMeasurementAttemptCount {

        @Test
        @DisplayName("성공 (공식 측정 5회)")
        void success() {
            // when
            int attemptCount = problemOfficialCostPolicy.getMeasurementAttemptCount();

            // then
            assertThat(attemptCount).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("selectMedianCostMeasurement")
    class SelectMedianCostMeasurement {

        @Test
        @DisplayName("성공 (Cost 중앙값 선택)")
        void successWhenCostsExist() {
            // given
            List<ProblemPlanMeasurement> measurements = List.of(
                    measurement(1, "10.0"),
                    measurement(2, "30.0"),
                    measurement(3, "20.0"),
                    measurement(4, "40.0"),
                    measurement(5, "50.0")
            );

            // when
            ProblemPlanMeasurement selectedMeasurement = problemOfficialCostPolicy.selectMedianCostMeasurement(measurements);

            // then
            assertThat(selectedMeasurement.getAttemptOrder()).isEqualTo(2);
            assertThat(selectedMeasurement.getCost()).isEqualByComparingTo("30.0");
        }

        @Test
        @DisplayName("성공 (Cost 없는 경우 첫 측정값 선택)")
        void successWhenCostsMissing() {
            // given
            List<ProblemPlanMeasurement> measurements = List.of(
                    new ProblemPlanMeasurement(1, null, List.of("plan1")),
                    new ProblemPlanMeasurement(2, null, List.of("plan2"))
            );

            // when
            ProblemPlanMeasurement selectedMeasurement = problemOfficialCostPolicy.selectMedianCostMeasurement(measurements);

            // then
            assertThat(selectedMeasurement.getAttemptOrder()).isEqualTo(1);
        }
    }

    private static ProblemPlanMeasurement measurement(int attemptOrder, String cost) {
        return new ProblemPlanMeasurement(attemptOrder, new BigDecimal(cost), List.of("plan" + attemptOrder));
    }
}
