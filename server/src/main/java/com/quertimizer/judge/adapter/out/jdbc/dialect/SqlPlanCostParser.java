package com.quertimizer.judge.adapter.out.jdbc.dialect;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;

public final class SqlPlanCostParser {

    private SqlPlanCostParser() {
    }

    public static BigDecimal extractEstimatedCost(List<String> planLines) {
        BigDecimal mySqlTotalCost = null;

        for (String planLine : planLines) {
            if (planLine == null || planLine.isBlank()) {
                continue;
            }

            Matcher postgreSqlMatcher = SqlPlanCostPatterns.POSTGRESQL_TOTAL_COST.matcher(planLine);
            if (postgreSqlMatcher.find()) {
                return parseBigDecimal(postgreSqlMatcher.group(1));
            }

            Matcher mySqlMatcher = SqlPlanCostPatterns.MYSQL_QUERY_COST.matcher(planLine);
            while (mySqlMatcher.find()) {
                BigDecimal queryCost = parseBigDecimal(mySqlMatcher.group(1));
                if (queryCost != null) {
                    mySqlTotalCost = mySqlTotalCost == null ? queryCost : mySqlTotalCost.add(queryCost);
                }
            }
        }

        return mySqlTotalCost;
    }

    private static BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
