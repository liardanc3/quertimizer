package com.quertimizer.global.constant;

public final class PostgreSqlExecutionPlanElementIndex {

    public static final int FULL_SCAN = 0;
    public static final int INDEX_SCAN = 1;
    public static final int INDEX_ONLY_SCAN = 2;
    public static final int BITMAP_INDEX_SCAN = 3;
    public static final int BITMAP_HEAP_SCAN = 4;
    public static final int SEQ_SCAN = 5;
    public static final int TID_SCAN = 6;
    public static final int SUBQUERY_SCAN = 7;
    public static final int CTE_SCAN = 8;
    public static final int FUNCTION_SCAN = 9;
    public static final int VALUES_SCAN = 10;
    public static final int HASH_JOIN = 11;
    public static final int MERGE_JOIN = 12;
    public static final int NESTED_LOOP = 13;
    public static final int HASH_AGGREGATE = 14;
    public static final int GROUP_AGGREGATE = 15;
    public static final int SORT = 16;
    public static final int INCREMENTAL_SORT = 17;
    public static final int LIMIT = 18;
    public static final int UNIQUE = 19;
    public static final int MATERIALIZE = 20;
    public static final int MEMOIZE = 21;
    public static final int APPEND = 22;
    public static final int MERGE_APPEND = 23;
    public static final int GATHER = 24;
    public static final int GATHER_MERGE = 25;
    public static final int PARALLEL = 26;
    public static final int PARTITION_PRUNING = 27;
    public static final int FILTER = 28;
    public static final int INDEX_CONDITION = 29;
    public static final int HINT = 30;

    private PostgreSqlExecutionPlanElementIndex() {
    }

}
