package com.quertimizer.mock;

import com.quertimizer.entity.Problem;
import com.quertimizer.repository.ProblemRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component("problemMockData")
@DependsOn("problemSetMockData")
@RequiredArgsConstructor
public class ProblemMockData {

    private static final String DESCRIPTION = """
            2024년 3월에 발생한 주문을 기준으로 고객별 주문 건수와 총 주문 금액을 조회하라.
            주문 일시는 orders.ordered_at 기준으로 판단한다.
            2024년 3월에 주문이 없는 고객은 결과에서 제외한다.
            """;

    private static final String CONDITION = """
            orders.ordered_at >= '2024-03-01 00:00:00'
            orders.ordered_at < '2024-04-01 00:00:00'
            고객별 주문 건수는 COUNT(DISTINCT orders.order_id)로 계산한다.
            고객별 총 주문 금액은 SUM(order_items.quantity * order_items.unit_price)로 계산한다.
            결과는 total_amount 내림차순, customer_id 오름차순으로 정렬한다.
            """;

    private static final String OUTPUT = """
            customer_id: 고객 ID
            customer_name: 고객 이름
            order_count: 2024년 3월 주문 건수
            total_amount: 2024년 3월 총 주문 금액
            """;

    private static final String OUTPUT_SAMPLE = """
            customer_id,customer_name,order_count,total_amount
            1,고객00001,2,134000.00
            2,고객00002,1,78000.00
            """;

    private static final String ANSWER = """
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

    private final ProblemRepository problemRepository;

    @PostConstruct
    public void seed() {
        problemRepository.save(Problem.create(
                "00001-00001",
                "00001",
                "3월 고객별 주문 건수와 총 주문 금액 조회",
                DESCRIPTION,
                ProblemSetMockData.TABLE_SET_00001_POSTGRESQL_DDL,
                ProblemSetMockData.TABLE_SET_00001_ORACLE_DDL,
                CONDITION,
                OUTPUT,
                OUTPUT_SAMPLE,
                ANSWER
        ));
    }
}
