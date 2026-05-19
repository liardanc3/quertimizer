package com.quertimizer.judge.adapter.out.jdbc.dialect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqlPlanCostParser")
class SqlPlanCostParserTest {

    @Nested
    @DisplayName("extractEstimatedCost")
    class ExtractEstimatedCost {

        @Test
        @DisplayName("성공 (MySQL query_cost 누적)")
        void successWhenMySqlQueryCostsAreSummed() {
            // given
            List<String> planLines = List.of("""
                    {
                      "query_block": {
                        "cost_info": {
                          "query_cost": "3.15"
                        },
                        "nested_loop": [
                          {
                            "query_block": {
                              "cost_info": {
                                "query_cost": "7991.95"
                              }
                            }
                          },
                          {
                            "materialized_from_subquery": {
                              "query_block": {
                                "cost_info": {
                                  "query_cost": "102.40"
                                }
                              }
                            }
                          }
                        ]
                      }
                    }
                    """);

            // when
            BigDecimal cost = SqlPlanCostParser.extractEstimatedCost(planLines);

            // then
            assertThat(cost).isEqualByComparingTo("8097.50");
        }

        @Test
        @DisplayName("성공 (MySQL 여러 라인 query_cost 누적)")
        void successWhenMySqlQueryCostsAreSpreadAcrossLines() {
            // given
            List<String> planLines = List.of(
                    "\"query_cost\": \"1.20\"",
                    "\"query_cost\": 2.30"
            );

            // when
            BigDecimal cost = SqlPlanCostParser.extractEstimatedCost(planLines);

            // then
            assertThat(cost).isEqualByComparingTo("3.50");
        }

        @Test
        @DisplayName("성공 (PostgreSQL total cost 유지)")
        void successWhenPostgreSqlTotalCostIsExtracted() {
            // given
            List<String> planLines = List.of("Seq Scan on sensor_logs  (cost=0.00..123.45 rows=100 width=8)");

            // when
            BigDecimal cost = SqlPlanCostParser.extractEstimatedCost(planLines);

            // then
            assertThat(cost).isEqualByComparingTo("123.45");
        }

        @Test
        @DisplayName("성공 (비용 없음)")
        void successWhenCostDoesNotExist() {
            // given
            List<String> planLines = List.of("no cost");

            // when
            BigDecimal cost = SqlPlanCostParser.extractEstimatedCost(planLines);

            // then
            assertThat(cost).isNull();
        }
    }
}
