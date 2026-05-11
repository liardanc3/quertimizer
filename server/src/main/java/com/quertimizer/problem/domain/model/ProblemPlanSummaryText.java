package com.quertimizer.problem.domain.model;

public enum ProblemPlanSummaryText {

    HINT_USED("힌트를 사용했습니다."),
    SEQ_SCAN_INCLUDED("Seq Scan이 포함되었습니다."),
    INDEX_ONLY_SCAN_INCLUDED("Index Only Scan이 포함되었습니다."),
    BITMAP_INDEX_SCAN_INCLUDED("Bitmap Index Scan이 포함되었습니다."),
    BITMAP_HEAP_SCAN_INCLUDED("Bitmap Heap Scan이 포함되었습니다."),
    INDEX_SCAN_INCLUDED("Index Scan이 포함되었습니다."),
    TID_SCAN_INCLUDED("Tid Scan이 포함되었습니다."),
    SUBQUERY_SCAN_INCLUDED("Subquery Scan이 포함되었습니다."),
    CTE_SCAN_INCLUDED("CTE Scan이 포함되었습니다."),
    FUNCTION_SCAN_INCLUDED("Function Scan이 포함되었습니다."),
    VALUES_SCAN_INCLUDED("Values Scan이 포함되었습니다."),
    HASH_JOIN_INCLUDED("Hash Join이 포함되었습니다."),
    MERGE_JOIN_INCLUDED("Merge Join이 포함되었습니다."),
    NESTED_LOOP_INCLUDED("Nested Loop가 포함되었습니다."),
    HASH_AGGREGATE_INCLUDED("Hash Aggregate가 포함되었습니다."),
    GROUP_AGGREGATE_INCLUDED("Group Aggregate가 포함되었습니다."),
    INCREMENTAL_SORT_INCLUDED("Incremental Sort가 포함되었습니다."),
    SORT_INCLUDED("Sort가 포함되었습니다."),
    LIMIT_INCLUDED("Limit이 포함되었습니다."),
    UNIQUE_INCLUDED("Unique가 포함되었습니다."),
    MATERIALIZE_INCLUDED("Materialize가 포함되었습니다."),
    MEMOIZE_INCLUDED("Memoize가 포함되었습니다."),
    MERGE_APPEND_INCLUDED("Merge Append가 포함되었습니다."),
    APPEND_INCLUDED("Append가 포함되었습니다."),
    GATHER_MERGE_INCLUDED("Gather Merge가 포함되었습니다."),
    GATHER_INCLUDED("Gather가 포함되었습니다."),
    PARALLEL_INCLUDED("병렬 실행이 포함되었습니다."),
    PARTITION_PRUNING_INCLUDED("파티션 가지치기가 포함되었습니다."),
    INDEX_CONDITION_INCLUDED("인덱스 접근 조건이 포함되었습니다."),
    POST_FILTER_INCLUDED("후처리 필터가 포함되었습니다."),
    REPRESENTATIVE_ELEMENT_NOT_FOUND("대표 실행계획 요소를 찾지 못했습니다.");

    private final String text;

    ProblemPlanSummaryText(String text) {
        this.text = text;
    }

    public String getText() {
        // 문구 조회
        return text;
    }

}
