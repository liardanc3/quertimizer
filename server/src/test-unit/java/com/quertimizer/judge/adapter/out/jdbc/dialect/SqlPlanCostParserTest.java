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
        @DisplayName("성공 (MySQL query_block query_cost 합산)")
        void successWhenMySqlQueryBlockCostsExist() {
            // given
            List<String> planLines = List.of("""
                    {
                      "query_block": {
                        "cost_info": {
                          "query_cost": "3.15"
                        },
                        "nested_loop": [
                          {
                            "materialized_from_subquery": {
                              "query_block": {
                                "cost_info": {
                                  "query_cost": "102.40"
                                },
                                "table": {
                                  "cost_info": {
                                    "read_cost": "11.20",
                                    "eval_cost": "9.10",
                                    "prefix_cost": "102.40"
                                  }
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
            assertThat(cost).isEqualByComparingTo("105.55");
        }

        @Test
        @DisplayName("성공 (MySQL JSON 파싱 실패 시 첫 query_cost 사용)")
        void successWhenMySqlPlanIsNotJson() {
            // given
            List<String> planLines = List.of(
                    "\"query_cost\": \"1.20\"",
                    "\"query_cost\": 2.30"
            );

            // when
            BigDecimal cost = SqlPlanCostParser.extractEstimatedCost(planLines);

            // then
            assertThat(cost).isEqualByComparingTo("1.20");
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
