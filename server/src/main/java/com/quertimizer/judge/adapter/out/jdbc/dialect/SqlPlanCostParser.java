package com.quertimizer.judge.adapter.out.jdbc.dialect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;

public final class SqlPlanCostParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SqlPlanCostParser() {
    }

    public static BigDecimal extractEstimatedCost(List<String> planLines) {
        BigDecimal mySqlFallbackCost = null;

        for (String planLine : planLines) {
            if (planLine == null || planLine.isBlank()) {
                continue;
            }

            Matcher postgreSqlMatcher = SqlPlanCostPatterns.POSTGRESQL_TOTAL_COST.matcher(planLine);
            if (postgreSqlMatcher.find()) {
                return parseBigDecimal(postgreSqlMatcher.group(1));
            }

            BigDecimal mySqlRootCost = extractMySqlRootQueryCost(planLine);
            if (mySqlRootCost != null) {
                return mySqlRootCost;
            }

            Matcher mySqlMatcher = SqlPlanCostPatterns.MYSQL_QUERY_COST.matcher(planLine);
            if (mySqlFallbackCost == null && mySqlMatcher.find()) {
                mySqlFallbackCost = parseBigDecimal(mySqlMatcher.group(1));
            }
        }

        return mySqlFallbackCost;
    }

    private static BigDecimal extractMySqlRootQueryCost(String planLine) {
        try {
            JsonNode costNode = OBJECT_MAPPER.readTree(planLine)
                    .path("query_block")
                    .path("cost_info")
                    .path("query_cost");
            return costNode.isMissingNode() ? null : parseBigDecimal(costNode.asText());
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private static BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
