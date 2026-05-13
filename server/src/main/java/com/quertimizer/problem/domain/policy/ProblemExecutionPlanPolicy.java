package com.quertimizer.problem.domain.policy;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.domain.model.MySqlExecutionPlanElementIndex;
import com.quertimizer.problem.domain.model.PostgreSqlExecutionPlanElementIndex;
import com.quertimizer.problem.domain.model.ProblemExecutionPlanAnalysis;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import static com.quertimizer.problem.domain.model.ProblemPlanSummaryText.*;
import static com.quertimizer.problem.domain.model.ProblemSubmitProgressText.*;
import static com.quertimizer.problem.domain.policy.ProblemExecutionPlanTokens.*;

@Component
public class ProblemExecutionPlanPolicy {
    public ProblemExecutionPlanAnalysis analyze(DbmsType dbmsType, List<String> planLines, String submittedSql) {
        // DBMS별 실행 계획 표현 차이를 각 DBMS 전용 분석 흐름으로 위임
        return switch (dbmsType) {
            case POSTGRESQL -> analyzePostgreSqlPlan(planLines, submittedSql);
            case MYSQL -> analyzeMySqlPlan(planLines, submittedSql);
        };
    }

    public List<String> resolveDetailLines(DbmsType dbmsType, long executionPlanElement) {
        // DBMS별 대표 요소 비트의 의미를 진행 상태 문구로 변환
        if (dbmsType == DbmsType.MYSQL) {
            return resolveMySqlDetailLines(executionPlanElement);
        }

        return resolvePostgreSqlDetailLines(executionPlanElement);
    }

    private ProblemExecutionPlanAnalysis analyzePostgreSqlPlan(List<String> planLines, String submittedSql) {
        // PostgreSQL 실행 계획 라인을 대표 연산 단위로 분류
        long executionPlanElement = 0L;
        LinkedHashSet<String> summaryLines = new LinkedHashSet<>();

        if (usesOptimizerHint(submittedSql)) {
            executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HINT);
            summaryLines.add(HINT_USED.getText());
        }

        for (String planLine : safePlanLines(planLines)) {
            String normalizedLine = planLine.trim().toUpperCase(Locale.ROOT);
            if (normalizedLine.isBlank()) {
                continue;
            }

            executionPlanElement = appendPostgreSqlScanElements(executionPlanElement, summaryLines, normalizedLine);
            executionPlanElement = appendPostgreSqlJoinElements(executionPlanElement, summaryLines, normalizedLine);
            executionPlanElement = appendPostgreSqlAggregateAndSortElements(executionPlanElement, summaryLines, normalizedLine);
            executionPlanElement = appendPostgreSqlOtherElements(executionPlanElement, summaryLines, normalizedLine);
        }

        if (summaryLines.isEmpty()) {
            summaryLines.add(REPRESENTATIVE_ELEMENT_NOT_FOUND.getText());
        }

        return new ProblemExecutionPlanAnalysis(executionPlanElement, List.copyOf(summaryLines));
    }

    private long appendPostgreSqlScanElements(long executionPlanElement, LinkedHashSet<String> summaryLines,
                                              String normalizedLine) {
        // PostgreSQL scan 계열 실행 계획 요소 추출
        long result = executionPlanElement;
        if (normalizedLine.contains("SEQ SCAN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.FULL_SCAN);
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.SEQ_SCAN);
            summaryLines.add(SEQ_SCAN_INCLUDED.getText());
        }

        if (normalizedLine.contains("INDEX ONLY SCAN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.INDEX_ONLY_SCAN);
            summaryLines.add(INDEX_ONLY_SCAN_INCLUDED.getText());
        } else if (normalizedLine.contains("BITMAP INDEX SCAN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN);
            summaryLines.add(BITMAP_INDEX_SCAN_INCLUDED.getText());
        } else if (normalizedLine.contains("BITMAP HEAP SCAN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.BITMAP_HEAP_SCAN);
            summaryLines.add(BITMAP_HEAP_SCAN_INCLUDED.getText());
        } else if (normalizedLine.contains("INDEX SCAN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.INDEX_SCAN);
            summaryLines.add(INDEX_SCAN_INCLUDED.getText());
        }

        if (normalizedLine.contains("TID SCAN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.TID_SCAN);
            summaryLines.add(TID_SCAN_INCLUDED.getText());
        }

        if (normalizedLine.contains("SUBQUERY SCAN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.SUBQUERY_SCAN);
            summaryLines.add(SUBQUERY_SCAN_INCLUDED.getText());
        }

        if (normalizedLine.contains("CTE SCAN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.CTE_SCAN);
            summaryLines.add(CTE_SCAN_INCLUDED.getText());
        }

        if (normalizedLine.contains("FUNCTION SCAN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.FUNCTION_SCAN);
            summaryLines.add(FUNCTION_SCAN_INCLUDED.getText());
        }

        if (normalizedLine.contains("VALUES SCAN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.VALUES_SCAN);
            summaryLines.add(VALUES_SCAN_INCLUDED.getText());
        }

        return result;
    }

    private long appendPostgreSqlJoinElements(long executionPlanElement, LinkedHashSet<String> summaryLines,
                                              String normalizedLine) {
        // PostgreSQL join 계열 실행 계획 요소 추출
        long result = executionPlanElement;
        if (normalizedLine.contains("HASH JOIN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.HASH_JOIN);
            summaryLines.add(HASH_JOIN_INCLUDED.getText());
        }

        if (normalizedLine.contains("MERGE JOIN")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.MERGE_JOIN);
            summaryLines.add(MERGE_JOIN_INCLUDED.getText());
        }

        if (normalizedLine.contains("NESTED LOOP")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.NESTED_LOOP);
            summaryLines.add(NESTED_LOOP_INCLUDED.getText());
        }

        return result;
    }

    private long appendPostgreSqlAggregateAndSortElements(long executionPlanElement, LinkedHashSet<String> summaryLines,
                                                          String normalizedLine) {
        // PostgreSQL aggregate/sort 계열 실행 계획 요소 추출
        long result = executionPlanElement;
        if (containsAny(normalizedLine, POSTGRESQL_HASH_AGGREGATE)) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.HASH_AGGREGATE);
            summaryLines.add(HASH_AGGREGATE_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, POSTGRESQL_GROUP_AGGREGATE)) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.GROUP_AGGREGATE);
            summaryLines.add(GROUP_AGGREGATE_INCLUDED.getText());
        }

        if (normalizedLine.contains("INCREMENTAL SORT")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.INCREMENTAL_SORT);
            summaryLines.add(INCREMENTAL_SORT_INCLUDED.getText());
        } else if (normalizedLine.contains("SORT")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.SORT);
            summaryLines.add(SORT_INCLUDED.getText());
        }

        if (normalizedLine.contains("UNIQUE") && !normalizedLine.contains("INNER UNIQUE")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.UNIQUE);
            summaryLines.add(UNIQUE_INCLUDED.getText());
        }

        return result;
    }

    private long appendPostgreSqlOtherElements(long executionPlanElement, LinkedHashSet<String> summaryLines,
                                               String normalizedLine) {
        // PostgreSQL 기타 실행 계획 요소와 조건 요소 추출
        long result = executionPlanElement;
        if (normalizedLine.contains("LIMIT")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.LIMIT);
            summaryLines.add(LIMIT_INCLUDED.getText());
        }

        if (normalizedLine.contains("MATERIALIZE")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.MATERIALIZE);
            summaryLines.add(MATERIALIZE_INCLUDED.getText());
        }

        if (normalizedLine.contains("MEMOIZE")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.MEMOIZE);
            summaryLines.add(MEMOIZE_INCLUDED.getText());
        }

        if (normalizedLine.contains("MERGE APPEND")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.MERGE_APPEND);
            summaryLines.add(MERGE_APPEND_INCLUDED.getText());
        } else if (normalizedLine.contains("APPEND")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.APPEND);
            summaryLines.add(APPEND_INCLUDED.getText());
        }

        if (normalizedLine.contains("GATHER MERGE")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.GATHER_MERGE);
            summaryLines.add(GATHER_MERGE_INCLUDED.getText());
        } else if (normalizedLine.contains("GATHER")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.GATHER);
            summaryLines.add(GATHER_INCLUDED.getText());
        }

        if (normalizedLine.contains("PARALLEL")) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.PARALLEL);
            summaryLines.add(PARALLEL_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, POSTGRESQL_PARTITION_PRUNING)) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.PARTITION_PRUNING);
            summaryLines.add(PARTITION_PRUNING_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, POSTGRESQL_INDEX_CONDITION)) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.INDEX_CONDITION);
            summaryLines.add(INDEX_CONDITION_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, POSTGRESQL_FILTER)) {
            result = appendPlanElement(result, PostgreSqlExecutionPlanElementIndex.FILTER);
            summaryLines.add(POST_FILTER_INCLUDED.getText());
        }

        return result;
    }

    private ProblemExecutionPlanAnalysis analyzeMySqlPlan(List<String> planLines, String submittedSql) {
        // MySQL 실행 계획 라인을 대표 연산 단위로 분류
        long executionPlanElement = 0L;
        LinkedHashSet<String> summaryLines = new LinkedHashSet<>();

        if (usesOptimizerHint(submittedSql)) {
            executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.HINT);
            summaryLines.add(HINT_USED.getText());
        }

        for (String planLine : safePlanLines(planLines)) {
            String normalizedLine = planLine.trim().toUpperCase(Locale.ROOT);
            if (normalizedLine.isBlank()) {
                continue;
            }

            executionPlanElement = appendMySqlScanElements(executionPlanElement, summaryLines, normalizedLine);
            executionPlanElement = appendMySqlJoinAndFilterElements(executionPlanElement, summaryLines, normalizedLine);
            executionPlanElement = appendMySqlSortAndAggregateElements(executionPlanElement, summaryLines, normalizedLine);
        }

        if (summaryLines.isEmpty()) {
            summaryLines.add(REPRESENTATIVE_ELEMENT_NOT_FOUND.getText());
        }

        return new ProblemExecutionPlanAnalysis(executionPlanElement, List.copyOf(summaryLines));
    }

    private long appendMySqlScanElements(long executionPlanElement, LinkedHashSet<String> summaryLines,
                                         String normalizedLine) {
        // MySQL access type과 derived table 계열 실행 계획 요소 추출
        long result = executionPlanElement;
        if (containsAny(normalizedLine, MYSQL_FULL_TABLE_SCAN)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.FULL_TABLE_SCAN);
            summaryLines.add(SEQ_SCAN_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_INDEX_SCAN)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.INDEX_SCAN);
            summaryLines.add(INDEX_SCAN_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_RANGE_SCAN)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.RANGE_SCAN);
            summaryLines.add(INDEX_SCAN_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_REF_SCAN)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.REF_SCAN);
            summaryLines.add(INDEX_SCAN_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_EQ_REF_SCAN)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.EQ_REF_SCAN);
            summaryLines.add(INDEX_SCAN_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_CONST_SCAN)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.CONST_SCAN);
            summaryLines.add(INDEX_SCAN_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_INDEX_MERGE)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.INDEX_MERGE);
            summaryLines.add(INDEX_SCAN_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_DERIVED)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.DERIVED_TABLE);
            summaryLines.add(SUBQUERY_SCAN_INCLUDED.getText());
        }

        if (normalizedLine.contains("MATERIALIZED")) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.MATERIALIZED_SUBQUERY);
            summaryLines.add(MATERIALIZE_INCLUDED.getText());
        }

        return result;
    }

    private long appendMySqlJoinAndFilterElements(long executionPlanElement, LinkedHashSet<String> summaryLines,
                                                  String normalizedLine) {
        // MySQL join/filter 계열 실행 계획 요소 추출
        long result = executionPlanElement;
        if (containsAny(normalizedLine, MYSQL_NESTED_LOOP)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.NESTED_LOOP_JOIN);
            summaryLines.add(NESTED_LOOP_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_HASH_JOIN)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.HASH_JOIN);
            summaryLines.add(HASH_JOIN_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_INDEX_CONDITION)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.INDEX_CONDITION);
            summaryLines.add(INDEX_CONDITION_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_FILTER)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.FILTER_CONDITION);
            summaryLines.add(POST_FILTER_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_JOIN_BUFFER)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.USING_JOIN_BUFFER);
            summaryLines.add(NESTED_LOOP_INCLUDED.getText());
        }

        return result;
    }

    private long appendMySqlSortAndAggregateElements(long executionPlanElement, LinkedHashSet<String> summaryLines,
                                                     String normalizedLine) {
        // MySQL sort/aggregate 계열 실행 계획 요소 추출
        long result = executionPlanElement;
        if (containsAny(normalizedLine, MYSQL_FILESORT)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.FILESORT);
            summaryLines.add(SORT_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_TEMPORARY_TABLE)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.TEMPORARY_TABLE);
            summaryLines.add(MATERIALIZE_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_GROUPING)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.GROUPING_OPERATION);
            summaryLines.add(GROUP_AGGREGATE_INCLUDED.getText());
        }

        if (containsAny(normalizedLine, MYSQL_WINDOW)) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.WINDOW_OPERATION);
            summaryLines.add(GROUP_AGGREGATE_INCLUDED.getText());
        }

        if (normalizedLine.contains("AGGREGATE")) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.AGGREGATE);
            summaryLines.add(GROUP_AGGREGATE_INCLUDED.getText());
        }

        if (normalizedLine.contains("LIMIT")) {
            result = appendPlanElement(result, MySqlExecutionPlanElementIndex.LIMIT);
            summaryLines.add(LIMIT_INCLUDED.getText());
        }

        return result;
    }

    private List<String> resolvePostgreSqlDetailLines(long executionPlanElement) {
        // PostgreSQL 대표 요소를 scan, join, filter, sort, aggregate 기준 상세 문구로 구성
        LinkedHashSet<String> detailLines = new LinkedHashSet<>();

        if (hasAnyPlanElement(executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.FULL_SCAN, PostgreSqlExecutionPlanElementIndex.SEQ_SCAN)) {
            detailLines.add(PLAN_FULL_SCAN.getText());
        }

        if (hasAnyPlanElement(executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.INDEX_SCAN, PostgreSqlExecutionPlanElementIndex.INDEX_ONLY_SCAN)) {
            detailLines.add(PLAN_INDEX_SCAN.getText());
        }

        if (hasAnyPlanElement(executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN, PostgreSqlExecutionPlanElementIndex.BITMAP_HEAP_SCAN)) {
            detailLines.add(PLAN_BITMAP_SCAN.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.TID_SCAN)) {
            detailLines.add(PLAN_TID_SCAN.getText());
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.SUBQUERY_SCAN,
                PostgreSqlExecutionPlanElementIndex.CTE_SCAN,
                PostgreSqlExecutionPlanElementIndex.FUNCTION_SCAN,
                PostgreSqlExecutionPlanElementIndex.VALUES_SCAN
        )) {
            detailLines.add(PLAN_DERIVED_SCAN.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.NESTED_LOOP)) {
            detailLines.add(PLAN_NESTED_LOOP.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.MERGE_JOIN)) {
            detailLines.add(PLAN_MERGE_JOIN.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HASH_JOIN)) {
            detailLines.add(PLAN_HASH_JOIN.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INDEX_CONDITION)) {
            detailLines.add(PLAN_ACCESS_FILTER.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.FILTER)) {
            detailLines.add(PLAN_POST_FILTER.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.SORT)) {
            detailLines.add(PLAN_PLAIN_SORT.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INCREMENTAL_SORT)) {
            detailLines.add(PLAN_INCREMENTAL_SORT.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HASH_AGGREGATE)) {
            detailLines.add(PLAN_HASH_AGG.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.GROUP_AGGREGATE)) {
            detailLines.add(PLAN_GROUP_AGG.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.UNIQUE)) {
            detailLines.add(PLAN_UNIQUE_AGG.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HINT)) {
            detailLines.add(PLAN_HINT_USED.getText());
        }

        return List.copyOf(detailLines);
    }

    private List<String> resolveMySqlDetailLines(long executionPlanElement) {
        // MySQL 대표 요소를 scan, join, filter, sort, aggregate 기준 상세 문구로 구성
        LinkedHashSet<String> detailLines = new LinkedHashSet<>();

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.FULL_TABLE_SCAN)) {
            detailLines.add(PLAN_FULL_SCAN.getText());
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                MySqlExecutionPlanElementIndex.INDEX_SCAN,
                MySqlExecutionPlanElementIndex.RANGE_SCAN,
                MySqlExecutionPlanElementIndex.REF_SCAN,
                MySqlExecutionPlanElementIndex.EQ_REF_SCAN,
                MySqlExecutionPlanElementIndex.CONST_SCAN,
                MySqlExecutionPlanElementIndex.INDEX_MERGE
        )) {
            detailLines.add(PLAN_INDEX_SCAN.getText());
        }

        if (hasAnyPlanElement(executionPlanElement,
                MySqlExecutionPlanElementIndex.DERIVED_TABLE, MySqlExecutionPlanElementIndex.MATERIALIZED_SUBQUERY)) {
            detailLines.add(PLAN_DERIVED_SCAN.getText());
        }

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.NESTED_LOOP_JOIN)) {
            detailLines.add(PLAN_NESTED_LOOP.getText());
        }

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.HASH_JOIN)) {
            detailLines.add(PLAN_HASH_JOIN.getText());
        }

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.INDEX_CONDITION)) {
            detailLines.add(PLAN_ACCESS_FILTER.getText());
        }

        if (hasAnyPlanElement(executionPlanElement,
                MySqlExecutionPlanElementIndex.FILTER_CONDITION, MySqlExecutionPlanElementIndex.ATTACHED_CONDITION)) {
            detailLines.add(PLAN_POST_FILTER.getText());
        }

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.FILESORT)) {
            detailLines.add(PLAN_PLAIN_SORT.getText());
        }

        if (hasAnyPlanElement(executionPlanElement,
                MySqlExecutionPlanElementIndex.GROUPING_OPERATION, MySqlExecutionPlanElementIndex.AGGREGATE)) {
            detailLines.add(PLAN_GROUP_AGG.getText());
        }

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.HINT)) {
            detailLines.add(PLAN_HINT_USED.getText());
        }

        return List.copyOf(detailLines);
    }

    private List<String> safePlanLines(List<String> planLines) {
        // null 실행 계획은 빈 목록으로 정리
        return planLines != null ? planLines : List.of();
    }

    private boolean usesOptimizerHint(String submittedSql) {
        // 제출 SQL의 optimizer hint 주석 포함 여부 확인
        return submittedSql != null && submittedSql.contains("/*+");
    }

    private long appendPlanElement(long executionPlanElement, int index) {
        // 실행 계획 대표 요소 bit set에 새 요소 추가
        return executionPlanElement | (1L << index);
    }

    private boolean hasAnyPlanElement(long executionPlanElement, int... indexes) {
        // 지정된 대표 요소 중 하나라도 포함되어 있는지 확인
        for (int index : indexes) {
            if (hasPlanElement(executionPlanElement, index)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasPlanElement(long executionPlanElement, int index) {
        // 지정된 대표 요소 포함 여부 확인
        return (executionPlanElement & (1L << index)) != 0;
    }
}
