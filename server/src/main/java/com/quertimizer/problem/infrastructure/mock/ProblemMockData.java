package com.quertimizer.problem.infrastructure.mock;

import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.infrastructure.repository.ProblemRepository;
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

    private static final String ANSWER = "0ed060840788ca7700422dc92721117f3e1d40dd161e5b433458fc0baacc2a48d2137c0ffd1a91baf6d85259e9b43e469a6fe6592de6f0f8656b24be300aec39";

    private static final String ANSWER_SQL = """
            SELECT c.customer_id,
                   c.customer_name,
                   COUNT(DISTINCT o.order_id) AS order_count,
                   SUM(oi.quantity * oi.unit_price) AS total_amount
            FROM customers c
            JOIN orders o ON o.customer_id = c.customer_id
            JOIN order_items oi ON oi.order_id = o.order_id
            WHERE o.ordered_at >= '2024-03-01 00:00:00'
              AND o.ordered_at < '2024-04-01 00:00:00'
            GROUP BY c.customer_id, c.customer_name
            ORDER BY total_amount DESC, c.customer_id ASC;
            """;

    private final ProblemRepository problemRepository;

    @PostConstruct
    public void seed() {
        saveProblem("P00001-00001", "P00001", ProblemSetMockData.TABLE_SET_00001_POSTGRESQL_DDL, true, false);
        saveProblem("O00001-00001", "O00001", ProblemSetMockData.TABLE_SET_00001_ORACLE_DDL, false, true);
        saveProblem("P00001-00002", "P00001", ProblemSetMockData.TABLE_SET_00001_POSTGRESQL_DDL, true, false);
        saveProblem("O00001-00002", "O00001", ProblemSetMockData.TABLE_SET_00001_ORACLE_DDL, false, true);
    }

    private void saveProblem(String problemId, String problemSetId, String ddl, boolean isPostgresql, boolean isOracle) {
        problemRepository.save(Problem.create(
                problemId,
                problemSetId,
                "3월 고객별 주문 건수와 총 주문 금액 조회",
                DESCRIPTION,
                ddl,
                isPostgresql,
                isOracle,
                CONDITION,
                OUTPUT,
                OUTPUT_SAMPLE,
                ANSWER,
                ANSWER_SQL
        ));
    }
}
