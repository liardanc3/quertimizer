package com.quertimizer.problem.domain.policy;

import com.quertimizer.problem.domain.model.ProblemPlanMeasurement;

import java.util.Comparator;
import java.util.List;

import static com.quertimizer.problem.domain.model.ProblemOfficialCostConstant.MEASUREMENT_ATTEMPT_COUNT;

public class ProblemOfficialCostPolicy {
    public int getMeasurementAttemptCount() {
        return MEASUREMENT_ATTEMPT_COUNT;
    }

    public ProblemPlanMeasurement selectMedianCostMeasurement(List<ProblemPlanMeasurement> measurements) {
        if (measurements == null || measurements.isEmpty()) {
            throw new IllegalArgumentException("실행 계획 비용 측정 결과가 필요합니다.");
        }

        // 비용이 있는 측정값만 중앙값 후보로 사용하고 비용 추출 실패 시 첫 번째 계획 유지
        List<ProblemPlanMeasurement> costMeasurements = measurements.stream()
                .filter(measurement -> measurement.getCost() != null)
                .sorted(Comparator.comparing(ProblemPlanMeasurement::getCost))
                .toList();
        if (costMeasurements.isEmpty()) {
            return measurements.get(0);
        }

        return costMeasurements.get(costMeasurements.size() / 2);
    }
}
