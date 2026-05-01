package com.quertimizer.problem.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class ProblemPlanMeasurement {

    private final int attemptOrder;
    private final BigDecimal cost;
    private final List<String> planLines;

    public ProblemPlanMeasurement(int attemptOrder, BigDecimal cost, List<String> planLines) {
        // 측정 순서 양수 여부 검증
        if (attemptOrder <= 0) {
            throw new IllegalArgumentException("측정 순서는 양수여야 합니다.");
        }

        // 실행 계획 측정 결과 값 보관
        this.attemptOrder = attemptOrder;
        this.cost = cost;
        this.planLines = List.copyOf(Objects.requireNonNull(planLines, "실행 계획 라인이 필요합니다."));
    }

    public int getAttemptOrder() {
        return attemptOrder;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public List<String> getPlanLines() {
        return planLines;
    }
}
