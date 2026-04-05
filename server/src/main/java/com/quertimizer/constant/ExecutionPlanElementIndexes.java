package com.quertimizer.constant;

public final class ExecutionPlanElementIndexes {

    private static final int[] POSTGRESQL_INDEXES = {
            PostgreSqlExecutionPlanElementIndex.FULL_SCAN,
            PostgreSqlExecutionPlanElementIndex.INDEX_SCAN,
            PostgreSqlExecutionPlanElementIndex.INDEX_ONLY_SCAN,
            PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN,
            PostgreSqlExecutionPlanElementIndex.BITMAP_HEAP_SCAN,
            PostgreSqlExecutionPlanElementIndex.SEQ_SCAN,
            PostgreSqlExecutionPlanElementIndex.TID_SCAN,
            PostgreSqlExecutionPlanElementIndex.SUBQUERY_SCAN,
            PostgreSqlExecutionPlanElementIndex.CTE_SCAN,
            PostgreSqlExecutionPlanElementIndex.FUNCTION_SCAN,
            PostgreSqlExecutionPlanElementIndex.VALUES_SCAN,
            PostgreSqlExecutionPlanElementIndex.HASH_JOIN,
            PostgreSqlExecutionPlanElementIndex.MERGE_JOIN,
            PostgreSqlExecutionPlanElementIndex.NESTED_LOOP,
            PostgreSqlExecutionPlanElementIndex.HASH_AGGREGATE,
            PostgreSqlExecutionPlanElementIndex.GROUP_AGGREGATE,
            PostgreSqlExecutionPlanElementIndex.SORT,
            PostgreSqlExecutionPlanElementIndex.INCREMENTAL_SORT,
            PostgreSqlExecutionPlanElementIndex.LIMIT,
            PostgreSqlExecutionPlanElementIndex.UNIQUE,
            PostgreSqlExecutionPlanElementIndex.MATERIALIZE,
            PostgreSqlExecutionPlanElementIndex.MEMOIZE,
            PostgreSqlExecutionPlanElementIndex.APPEND,
            PostgreSqlExecutionPlanElementIndex.MERGE_APPEND,
            PostgreSqlExecutionPlanElementIndex.GATHER,
            PostgreSqlExecutionPlanElementIndex.GATHER_MERGE,
            PostgreSqlExecutionPlanElementIndex.PARALLEL,
            PostgreSqlExecutionPlanElementIndex.PARTITION_PRUNING,
            PostgreSqlExecutionPlanElementIndex.FILTER,
            PostgreSqlExecutionPlanElementIndex.INDEX_CONDITION,
            PostgreSqlExecutionPlanElementIndex.HINT
    };

    private static final int[] ORACLE_INDEXES = {
            OracleExecutionPlanElementIndex.FULL_SCAN,
            OracleExecutionPlanElementIndex.ROWID_ACCESS,
            OracleExecutionPlanElementIndex.INDEX_SCAN,
            OracleExecutionPlanElementIndex.BITMAP_SCAN,
            OracleExecutionPlanElementIndex.DERIVED_SCAN,
            OracleExecutionPlanElementIndex.REMOTE_SCAN,
            OracleExecutionPlanElementIndex.NESTED_LOOP,
            OracleExecutionPlanElementIndex.MERGE_JOIN,
            OracleExecutionPlanElementIndex.HASH_JOIN,
            OracleExecutionPlanElementIndex.CARTESIAN_JOIN,
            OracleExecutionPlanElementIndex.ACCESS_FILTER,
            OracleExecutionPlanElementIndex.POST_FILTER,
            OracleExecutionPlanElementIndex.JOIN_FILTER,
            OracleExecutionPlanElementIndex.ORDER_SORT,
            OracleExecutionPlanElementIndex.GROUP_SORT,
            OracleExecutionPlanElementIndex.UNIQUE_SORT,
            OracleExecutionPlanElementIndex.WINDOW_SORT,
            OracleExecutionPlanElementIndex.PLAIN_AGGREGATE,
            OracleExecutionPlanElementIndex.GROUP_AGGREGATE,
            OracleExecutionPlanElementIndex.HASH_AGGREGATE,
            OracleExecutionPlanElementIndex.WINDOW_AGGREGATE,
            OracleExecutionPlanElementIndex.HINT
    };

    private ExecutionPlanElementIndexes() {
    }

    public static long normalize(DbmsType dbmsType, long executionPlanElement) {

        // DBMS별로 정의된 실행계획 요소 비트만 남기고, 다른 DBMS 전용 비트는 응답에서 제거합니다.
        long normalizedExecutionPlanElement = 0L;
        for (int index : getIndexes(dbmsType)) {
            if ((executionPlanElement & (1L << index)) != 0) {
                normalizedExecutionPlanElement |= 1L << index;
            }
        }

        return normalizedExecutionPlanElement;
    }

    private static int[] getIndexes(DbmsType dbmsType) {
        return dbmsType == DbmsType.ORACLE ? ORACLE_INDEXES : POSTGRESQL_INDEXES;
    }

}
