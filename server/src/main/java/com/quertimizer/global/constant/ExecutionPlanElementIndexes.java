package com.quertimizer.global.constant;

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

    private static final int[] MYSQL_INDEXES = {
            MySqlExecutionPlanElementIndex.FULL_TABLE_SCAN,
            MySqlExecutionPlanElementIndex.INDEX_SCAN,
            MySqlExecutionPlanElementIndex.RANGE_SCAN,
            MySqlExecutionPlanElementIndex.REF_SCAN,
            MySqlExecutionPlanElementIndex.EQ_REF_SCAN,
            MySqlExecutionPlanElementIndex.CONST_SCAN,
            MySqlExecutionPlanElementIndex.INDEX_MERGE,
            MySqlExecutionPlanElementIndex.DERIVED_TABLE,
            MySqlExecutionPlanElementIndex.MATERIALIZED_SUBQUERY,
            MySqlExecutionPlanElementIndex.NESTED_LOOP_JOIN,
            MySqlExecutionPlanElementIndex.HASH_JOIN,
            MySqlExecutionPlanElementIndex.FILTER_CONDITION,
            MySqlExecutionPlanElementIndex.INDEX_CONDITION,
            MySqlExecutionPlanElementIndex.ATTACHED_CONDITION,
            MySqlExecutionPlanElementIndex.FILESORT,
            MySqlExecutionPlanElementIndex.TEMPORARY_TABLE,
            MySqlExecutionPlanElementIndex.GROUPING_OPERATION,
            MySqlExecutionPlanElementIndex.WINDOW_OPERATION,
            MySqlExecutionPlanElementIndex.AGGREGATE,
            MySqlExecutionPlanElementIndex.LIMIT,
            MySqlExecutionPlanElementIndex.USING_JOIN_BUFFER,
            MySqlExecutionPlanElementIndex.HINT
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
        // Indexes 조회
        return switch (dbmsType) {
            case POSTGRESQL -> POSTGRESQL_INDEXES;
            case MYSQL -> MYSQL_INDEXES;
        };
    }

}
