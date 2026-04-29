package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Indicates that an execution plan has been estimated.
 */
public class PlanEstimated extends AbstractSqlJudgeEvent {

    private final BigDecimal cost;
    private final List<String> planLines;

    /**
     * Creates an execution plan estimated event.
     *
     * @param executionId execution task ID
     * @param cost estimated cost
     * @param planLines execution plan lines
     */
    public PlanEstimated(JudgeExecutionId executionId, BigDecimal cost, List<String> planLines) {
        super(executionId);
        this.cost = cost;
        this.planLines = List.copyOf(Objects.requireNonNull(planLines, "planLines must not be null"));
    }

    /**
     * Returns the estimated cost.
     *
     * @return estimated cost
     */
    public BigDecimal getCost() {
        return cost;
    }

    /**
     * Returns the execution plan lines.
     *
     * @return execution plan lines
     */
    public List<String> getPlanLines() {
        return planLines;
    }
}
