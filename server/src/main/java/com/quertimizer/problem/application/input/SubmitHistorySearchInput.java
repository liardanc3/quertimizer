package com.quertimizer.problem.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SubmitHistorySearchInput {

    private final int requestedPage;
    private final String submitId;
    private final String query;
    private final String dbms;
    private final String problemId;
    private final String judge;
    private final String costSort;
    private final String planMatchMode;
    private final String scanBuckets;
    private final String joinBuckets;
    private final String filterBuckets;
    private final String sortBuckets;
    private final String aggregateBuckets;
    private final String hintFilters;
    private final String postgresqlScanBuckets;
    private final String postgresqlJoinBuckets;
    private final String postgresqlFilterBuckets;
    private final String postgresqlSortBuckets;
    private final String postgresqlAggregateBuckets;
    private final String postgresqlHintFilters;
    private final String mysqlScanBuckets;
    private final String mysqlJoinBuckets;
    private final String mysqlFilterBuckets;
    private final String mysqlSortBuckets;
    private final String mysqlAggregateBuckets;
    private final String mysqlHintFilters;
}
