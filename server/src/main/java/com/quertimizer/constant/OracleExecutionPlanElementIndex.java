package com.quertimizer.constant;

public final class OracleExecutionPlanElementIndex {

    public static final int FULL_SCAN = 0;
    public static final int ROWID_ACCESS = 1;
    public static final int INDEX_SCAN = 2;
    public static final int BITMAP_SCAN = 3;
    public static final int DERIVED_SCAN = 4;
    public static final int REMOTE_SCAN = 5;
    public static final int NESTED_LOOP = 10;
    public static final int MERGE_JOIN = 11;
    public static final int HASH_JOIN = 12;
    public static final int CARTESIAN_JOIN = 13;
    public static final int ACCESS_FILTER = 14;
    public static final int POST_FILTER = 15;
    public static final int JOIN_FILTER = 16;
    public static final int ORDER_SORT = 17;
    public static final int GROUP_SORT = 18;
    public static final int UNIQUE_SORT = 19;
    public static final int WINDOW_SORT = 20;
    public static final int PLAIN_AGGREGATE = 21;
    public static final int GROUP_AGGREGATE = 22;
    public static final int HASH_AGGREGATE = 23;
    public static final int WINDOW_AGGREGATE = 24;
    public static final int HINT = 30;

    private OracleExecutionPlanElementIndex() {
    }

}
