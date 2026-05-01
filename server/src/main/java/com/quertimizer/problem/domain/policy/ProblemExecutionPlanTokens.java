package com.quertimizer.problem.domain.policy;

import java.util.Set;

final class ProblemExecutionPlanTokens {

    static final Set<String> POSTGRESQL_HASH_AGGREGATE = Set.of("HASHAGGREGATE", "HASH AGGREGATE");
    static final Set<String> POSTGRESQL_GROUP_AGGREGATE = Set.of("GROUPAGGREGATE", "GROUP AGGREGATE");
    static final Set<String> POSTGRESQL_PARTITION_PRUNING = Set.of("PARTITION PRUNING", "PARTITIONS REMOVED");
    static final Set<String> POSTGRESQL_INDEX_CONDITION = Set.of("INDEX COND:", "RECHECK COND:");
    static final Set<String> POSTGRESQL_FILTER = Set.of("FILTER:", "ROWS REMOVED BY FILTER", "JOIN FILTER:");

    static final Set<String> MYSQL_FULL_TABLE_SCAN =
            Set.of("TYPE=ALL", "\"ACCESS_TYPE\": \"ALL\"", "\"ACCESS_TYPE\":\"ALL\"");
    static final Set<String> MYSQL_INDEX_SCAN =
            Set.of("TYPE=INDEX", "\"ACCESS_TYPE\": \"INDEX\"", "\"ACCESS_TYPE\":\"INDEX\"");
    static final Set<String> MYSQL_RANGE_SCAN =
            Set.of("TYPE=RANGE", "\"ACCESS_TYPE\": \"RANGE\"", "\"ACCESS_TYPE\":\"RANGE\"");
    static final Set<String> MYSQL_REF_SCAN =
            Set.of("TYPE=REF", "\"ACCESS_TYPE\": \"REF\"", "\"ACCESS_TYPE\":\"REF\"");
    static final Set<String> MYSQL_EQ_REF_SCAN =
            Set.of("TYPE=EQ_REF", "\"ACCESS_TYPE\": \"EQ_REF\"", "\"ACCESS_TYPE\":\"EQ_REF\"");
    static final Set<String> MYSQL_CONST_SCAN =
            Set.of("TYPE=CONST", "\"ACCESS_TYPE\": \"CONST\"", "\"ACCESS_TYPE\":\"CONST\"");
    static final Set<String> MYSQL_INDEX_MERGE = Set.of("INDEX_MERGE", "INDEX MERGE");
    static final Set<String> MYSQL_DERIVED = Set.of("DERIVED", "DEPENDENT DERIVED");
    static final Set<String> MYSQL_NESTED_LOOP = Set.of("NESTED_LOOP", "NESTED LOOP");
    static final Set<String> MYSQL_HASH_JOIN = Set.of("HASH_JOIN", "HASH JOIN");
    static final Set<String> MYSQL_INDEX_CONDITION =
            Set.of("USING INDEX CONDITION", "INDEX_CONDITION", "INDEX CONDITION");
    static final Set<String> MYSQL_FILTER =
            Set.of("USING WHERE", "FILTER_CONDITION", "ATTACHED_CONDITION", "ATTACHED CONDITION");
    static final Set<String> MYSQL_FILESORT = Set.of("USING FILESORT", "FILESORT");
    static final Set<String> MYSQL_TEMPORARY_TABLE = Set.of("USING TEMPORARY", "TEMPORARY_TABLE", "TEMPORARY TABLE");
    static final Set<String> MYSQL_GROUPING = Set.of("GROUPING_OPERATION", "GROUPING OPERATION", "GROUP BY");
    static final Set<String> MYSQL_WINDOW = Set.of("WINDOWING", "WINDOW_OPERATION", "WINDOW OPERATION");
    static final Set<String> MYSQL_JOIN_BUFFER = Set.of("USING JOIN BUFFER", "JOIN_BUFFER", "JOIN BUFFER");

    private ProblemExecutionPlanTokens() {
    }

    static boolean containsAny(String normalizedLine, Set<String> tokens) {
        // 실행 계획 라인의 대표 토큰 포함 여부 확인
        for (String token : tokens) {
            if (normalizedLine.contains(token)) {
                return true;
            }
        }

        return false;
    }
}
