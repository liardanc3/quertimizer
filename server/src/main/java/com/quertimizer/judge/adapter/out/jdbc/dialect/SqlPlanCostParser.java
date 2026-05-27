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

            BigDecimal mySqlQueryBlockCost = extractMySqlQueryBlockCost(planLine);
            if (mySqlQueryBlockCost != null) {
                return mySqlQueryBlockCost;
            }

            Matcher mySqlMatcher = SqlPlanCostPatterns.MYSQL_QUERY_COST.matcher(planLine);
            if (mySqlFallbackCost == null && mySqlMatcher.find()) {
                mySqlFallbackCost = parseBigDecimal(mySqlMatcher.group(1));
            }
        }

        return mySqlFallbackCost;
    }

    private static BigDecimal extractMySqlQueryBlockCost(String planLine) {
        try {
            BigDecimal queryBlockCost = sumQueryBlockCost(OBJECT_MAPPER.readTree(planLine));
            return BigDecimal.ZERO.compareTo(queryBlockCost) == 0 ? null : queryBlockCost;
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private static BigDecimal sumQueryBlockCost(JsonNode node) {
        BigDecimal totalCost = BigDecimal.ZERO;

        if (node.has("query_block")) {
            JsonNode queryBlock = node.get("query_block");
            JsonNode costNode = queryBlock.path("cost_info").path("query_cost");
            BigDecimal queryCost = costNode.isMissingNode() ? null : parseBigDecimal(costNode.asText());
            totalCost = queryCost != null ? totalCost.add(queryCost) : totalCost;
            totalCost = totalCost.add(sumQueryBlockCost(queryBlock));
            return totalCost;
        }

        if (node.isObject() || node.isArray()) {
            for (JsonNode childNode : node) {
                totalCost = totalCost.add(sumQueryBlockCost(childNode));
            }
        }

        return totalCost;
    }

    private static BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
