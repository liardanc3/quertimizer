package com.quertimizer.problem.application.port.out;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;

@Value
public class SubmitHistorySearchCondition {

    String submitId;
    String query;
    DbmsType dbmsType;
    String problemId;
    Boolean success;
    PlanElementSearchCondition postgresqlPlanCondition;
    PlanElementSearchCondition mysqlPlanCondition;

    public boolean hasPlanCondition() {
        // 실행 계획 검색 조건 보유 여부 확인
        return postgresqlPlanCondition.hasConditions() || mysqlPlanCondition.hasConditions();
    }

    @Value
    @Accessors(fluent = true)
    public static class PlanElementSearchCondition {
        boolean matchAll;
        List<PlanElementCondition> conditions;

        public static PlanElementSearchCondition empty() {
            return new PlanElementSearchCondition(false, List.of());
        }

        public boolean hasConditions() {
            // 실행 계획 조건 보유 여부 확인
            return !conditions.isEmpty();
        }
    }

    @Value
    @Accessors(fluent = true)
    public static class PlanElementCondition {
        long mask;
        boolean matched;
    }
}
