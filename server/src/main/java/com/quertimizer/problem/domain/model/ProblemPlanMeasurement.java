package com.quertimizer.problem.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProblemPlanMeasurement {

    private final int attemptOrder;
    private final BigDecimal cost;
    private final List<String> planLines;

    public ProblemPlanMeasurement(int attemptOrder, BigDecimal cost, List<String> planLines) {
        // 실행 계획 측정 결과 값 보관
        this.attemptOrder = attemptOrder;
        this.cost = cost;
        this.planLines = List.copyOf(planLines);
    }
}
