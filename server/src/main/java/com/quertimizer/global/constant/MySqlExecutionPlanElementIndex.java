package com.quertimizer.global.constant;

public final class MySqlExecutionPlanElementIndex {

    public static final int FULL_TABLE_SCAN = 0;
    public static final int INDEX_SCAN = 1;
    public static final int RANGE_SCAN = 2;
    public static final int REF_SCAN = 3;
    public static final int EQ_REF_SCAN = 4;
    public static final int CONST_SCAN = 5;
    public static final int INDEX_MERGE = 6;
    public static final int DERIVED_TABLE = 7;
    public static final int MATERIALIZED_SUBQUERY = 8;
    public static final int NESTED_LOOP_JOIN = 10;
    public static final int HASH_JOIN = 11;
    public static final int FILTER_CONDITION = 14;
    public static final int INDEX_CONDITION = 15;
    public static final int ATTACHED_CONDITION = 16;
    public static final int FILESORT = 17;
    public static final int TEMPORARY_TABLE = 18;
    public static final int GROUPING_OPERATION = 19;
    public static final int WINDOW_OPERATION = 20;
    public static final int AGGREGATE = 21;
    public static final int LIMIT = 22;
    public static final int USING_JOIN_BUFFER = 23;
    public static final int HINT = 30;

    private MySqlExecutionPlanElementIndex() {
    }

}
