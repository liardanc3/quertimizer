package com.quertimizer.problem.adapter.in.web.request;

import com.quertimizer.problem.application.input.SubmitHistorySearchInput;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitHistorySearchRequest {

    @Min(0)
    @Max(1000)
    private int page = 1;

    @Size(max = 20)
    private String submitId;

    @Size(max = 100)
    private String query;

    @Pattern(regexp = "all|postgresql|mysql")
    private String dbms;

    @Size(max = 20)
    private String problemId;

    @Pattern(regexp = "all|success|fail")
    private String judge;

    @Pattern(regexp = "none|asc|desc")
    private String costSort = "none";

    @Pattern(regexp = "and|or")
    private String planMatchMode;

    @Size(max = 300)
    private String scanBuckets;
    @Size(max = 300)
    private String joinBuckets;
    @Size(max = 300)
    private String filterBuckets;
    @Size(max = 300)
    private String sortBuckets;
    @Size(max = 300)
    private String aggregateBuckets;
    @Size(max = 300)
    private String hintFilters;
    @Size(max = 300)
    private String postgresqlScanBuckets;
    @Size(max = 300)
    private String postgresqlJoinBuckets;
    @Size(max = 300)
    private String postgresqlFilterBuckets;
    @Size(max = 300)
    private String postgresqlSortBuckets;
    @Size(max = 300)
    private String postgresqlAggregateBuckets;
    @Size(max = 300)
    private String postgresqlHintFilters;
    @Size(max = 300)
    private String mysqlScanBuckets;
    @Size(max = 300)
    private String mysqlJoinBuckets;
    @Size(max = 300)
    private String mysqlFilterBuckets;
    @Size(max = 300)
    private String mysqlSortBuckets;
    @Size(max = 300)
    private String mysqlAggregateBuckets;
    @Size(max = 300)
    private String mysqlHintFilters;

    public SubmitHistorySearchInput toInput() {
        return new SubmitHistorySearchInput(
                page,
                submitId,
                query,
                dbms,
                problemId,
                judge,
                costSort,
                planMatchMode,
                scanBuckets,
                joinBuckets,
                filterBuckets,
                sortBuckets,
                aggregateBuckets,
                hintFilters,
                postgresqlScanBuckets,
                postgresqlJoinBuckets,
                postgresqlFilterBuckets,
                postgresqlSortBuckets,
                postgresqlAggregateBuckets,
                postgresqlHintFilters,
                mysqlScanBuckets,
                mysqlJoinBuckets,
                mysqlFilterBuckets,
                mysqlSortBuckets,
                mysqlAggregateBuckets,
                mysqlHintFilters
        );
    }
}
