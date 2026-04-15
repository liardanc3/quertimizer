package com.quertimizer.mock;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.constant.OracleExecutionPlanElementIndex;
import com.quertimizer.constant.PostgreSqlExecutionPlanElementIndex;
import com.quertimizer.entity.ProblemSolveHistory;
import com.quertimizer.repository.ProblemSolveHistoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component("problemSolveHistoryMockData")
@DependsOn({"userMockData", "problemMockData"})
@RequiredArgsConstructor
public class ProblemSolveHistoryMockData {

    private static final String QUERY = """
            SELECT
                c.customer_id,
                c.customer_name,
                COUNT(DISTINCT o.order_id) AS order_count,
                SUM(oi.quantity * oi.unit_price) AS total_amount
            FROM customers c
            JOIN orders o
                ON c.customer_id = o.customer_id
            JOIN order_items oi
                ON o.order_id = oi.order_id
            WHERE o.ordered_at >= TIMESTAMP '2024-03-01 00:00:00'
              AND o.ordered_at < TIMESTAMP '2024-04-01 00:00:00'
            GROUP BY
                c.customer_id,
                c.customer_name
            ORDER BY
                total_amount DESC,
                c.customer_id ASC
            """;

    private static final long POSTGRESQL_PLAN_A = bitMask(
            PostgreSqlExecutionPlanElementIndex.SEQ_SCAN,
            PostgreSqlExecutionPlanElementIndex.HASH_JOIN,
            PostgreSqlExecutionPlanElementIndex.HASH_AGGREGATE,
            PostgreSqlExecutionPlanElementIndex.SORT,
            PostgreSqlExecutionPlanElementIndex.FILTER
    );
    private static final long POSTGRESQL_PLAN_B = bitMask(
            PostgreSqlExecutionPlanElementIndex.INDEX_SCAN,
            PostgreSqlExecutionPlanElementIndex.NESTED_LOOP,
            PostgreSqlExecutionPlanElementIndex.GROUP_AGGREGATE,
            PostgreSqlExecutionPlanElementIndex.SORT,
            PostgreSqlExecutionPlanElementIndex.INDEX_CONDITION
    );
    private static final long ORACLE_PLAN_A = bitMask(
            OracleExecutionPlanElementIndex.FULL_SCAN,
            OracleExecutionPlanElementIndex.HASH_JOIN,
            OracleExecutionPlanElementIndex.HASH_AGGREGATE,
            OracleExecutionPlanElementIndex.ORDER_SORT,
            OracleExecutionPlanElementIndex.ACCESS_FILTER
    );
    private static final long ORACLE_PLAN_B = bitMask(
            OracleExecutionPlanElementIndex.ROWID_ACCESS,
            OracleExecutionPlanElementIndex.INDEX_SCAN,
            OracleExecutionPlanElementIndex.NESTED_LOOP,
            OracleExecutionPlanElementIndex.GROUP_AGGREGATE,
            OracleExecutionPlanElementIndex.POST_FILTER
    );

    private final ProblemSolveHistoryRepository problemSolveHistoryRepository;

    @PostConstruct
    public void seed() {
        problemSolveHistoryRepository.saveAll(createHistories());
    }

    private List<ProblemSolveHistory> createHistories() {
        List<ProblemSolveHistory> histories = new ArrayList<>();

        histories.add(createHistory("liardanc3", DbmsType.POSTGRESQL, 97, POSTGRESQL_PLAN_B, LocalDateTime.of(2026, 4, 5, 21, 10)));
        histories.add(createHistory("liardanc3", DbmsType.ORACLE, 121, ORACLE_PLAN_B, LocalDateTime.of(2026, 4, 5, 21, 11)));

        histories.add(createHistory("beginner01", DbmsType.POSTGRESQL, 244, POSTGRESQL_PLAN_A, LocalDateTime.of(2026, 4, 1, 10, 5)));
        histories.add(createHistory("beginner02", DbmsType.POSTGRESQL, 231, POSTGRESQL_PLAN_A, LocalDateTime.of(2026, 4, 1, 10, 18)));
        histories.add(createHistory("beginner03", DbmsType.POSTGRESQL, 218, POSTGRESQL_PLAN_A, LocalDateTime.of(2026, 4, 1, 10, 42)));
        histories.add(createHistory("beginner04", DbmsType.POSTGRESQL, 206, POSTGRESQL_PLAN_B, LocalDateTime.of(2026, 4, 1, 11, 2)));
        histories.add(createHistory("beginner05", DbmsType.POSTGRESQL, 193, POSTGRESQL_PLAN_B, LocalDateTime.of(2026, 4, 1, 11, 17)));
        histories.add(createHistory("beginner06", DbmsType.POSTGRESQL, 181, POSTGRESQL_PLAN_B, LocalDateTime.of(2026, 4, 1, 11, 36)));
        histories.add(createHistory("beginner07", DbmsType.ORACLE, 238, ORACLE_PLAN_A, LocalDateTime.of(2026, 4, 1, 12, 7)));
        histories.add(createHistory("beginner08", DbmsType.ORACLE, 226, ORACLE_PLAN_A, LocalDateTime.of(2026, 4, 1, 12, 28)));

        histories.add(createHistory("intermediate01", DbmsType.POSTGRESQL, 169, POSTGRESQL_PLAN_B, LocalDateTime.of(2026, 4, 2, 9, 12)));
        histories.add(createHistory("intermediate02", DbmsType.POSTGRESQL, 161, POSTGRESQL_PLAN_B, LocalDateTime.of(2026, 4, 2, 9, 27)));
        histories.add(createHistory("intermediate03", DbmsType.POSTGRESQL, 153, POSTGRESQL_PLAN_B, LocalDateTime.of(2026, 4, 2, 9, 44)));
        histories.add(createHistory("intermediate04", DbmsType.POSTGRESQL, 146, POSTGRESQL_PLAN_B, LocalDateTime.of(2026, 4, 2, 10, 3)));
        histories.add(createHistory("intermediate05", DbmsType.ORACLE, 184, ORACLE_PLAN_B, LocalDateTime.of(2026, 4, 2, 10, 21)));
        histories.add(createHistory("intermediate06", DbmsType.ORACLE, 176, ORACLE_PLAN_B, LocalDateTime.of(2026, 4, 2, 10, 49)));
        histories.add(createHistory("intermediate07", DbmsType.ORACLE, 168, ORACLE_PLAN_B, LocalDateTime.of(2026, 4, 2, 11, 13)));

        histories.add(createHistory("advanced01", DbmsType.POSTGRESQL, 132, POSTGRESQL_PLAN_B, LocalDateTime.of(2026, 4, 3, 14, 9)));
        histories.add(createHistory("advanced02", DbmsType.ORACLE, 149, ORACLE_PLAN_B, LocalDateTime.of(2026, 4, 3, 14, 31)));
        histories.add(createHistory("advanced03", DbmsType.ORACLE, 142, ORACLE_PLAN_B, LocalDateTime.of(2026, 4, 3, 14, 55)));
        histories.add(createHistory("advanced04", DbmsType.ORACLE, 137, ORACLE_PLAN_B, LocalDateTime.of(2026, 4, 3, 15, 18)));

        return histories;
    }

    private ProblemSolveHistory createHistory(String userId,
                                              DbmsType dbmsType,
                                              long executionTimeMs,
                                              long executionPlanElement,
                                              LocalDateTime submittedAt) {
        return ProblemSolveHistory.create(
                "00001-00001",
                userId,
                dbmsType,
                QUERY,
                executionTimeMs,
                executionTimeMs,
                0,
                executionPlanElement,
                submittedAt
        );
    }

    private static long bitMask(int... indexes) {
        long bitMask = 0L;

        for (int index : indexes) {
            bitMask |= 1L << index;
        }

        return bitMask;
    }
}
