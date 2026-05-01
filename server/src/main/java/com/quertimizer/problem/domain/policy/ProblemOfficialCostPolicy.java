package com.quertimizer.problem.domain.policy;

import com.quertimizer.problem.domain.model.ProblemPlanMeasurement;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 공식 제출의 실행 계획 비용 측정 횟수와 대표 측정값 선택 정책을 제공한다.
 */
@Component
public class ProblemOfficialCostPolicy {

    private static final int MEASUREMENT_ATTEMPT_COUNT = 5;

    /**
     * 공식 제출에서 수행할 실행 계획 비용 측정 횟수를 반환한다.
     *
     * @return 실행 계획 비용 측정 횟수
     */
    public int getMeasurementAttemptCount() {
        return MEASUREMENT_ATTEMPT_COUNT;
    }

    /**
     * 측정 결과 중 비용 중앙값에 해당하는 실행 계획 측정을 선택한다.
     *
     * @param measurements 실행 계획 측정 결과 목록
     * @return 공식 기록에 사용할 실행 계획 측정 결과
     */
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
