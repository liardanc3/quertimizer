package com.quertimizer.problem.infrastructure.mock;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.application.port.ProblemRepository;
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

    private static final String SAMPLE_DATA_SQL = """
            INSERT INTO customers (customer_id, customer_name, region, signup_date)
            VALUES
                (1, '고객00001', 'BUSAN', '2023-01-18'),
                (2, '고객00002', 'DAEGU', '2023-02-04');

            INSERT INTO orders (order_id, customer_id, ordered_at, order_status, payment_method)
            VALUES
                (1101, 1, '2024-03-03 09:12:00', 'DELIVERED', 'CARD'),
                (1102, 1, '2024-03-18 14:26:00', 'DELIVERED', 'WALLET'),
                (1201, 2, '2024-03-07 11:05:00', 'PENDING', 'BANK_TRANSFER');

            INSERT INTO order_items (order_item_id, order_id, product_category, quantity, unit_price, discount_amount)
            VALUES
                (1, 1101, 'LIVING', 2, 12000.00, 0.00),
                (2, 1101, 'FOOD', 1, 18000.00, 0.00),
                (3, 1102, 'DIGITAL', 1, 92000.00, 11040.00),
                (4, 1201, 'BEAUTY', 3, 26000.00, 0.00);
            """;

    private final ProblemRepository problemRepository;

    @PostConstruct
    public void seed() {
        // 기본 문제 Mock 데이터 적재
        saveProblem("P00001-00001", "P00001", ProblemSetMockData.TABLE_SET_00001_POSTGRESQL_DDL, DbmsType.POSTGRESQL);
        saveProblem("M00001-00001", "M00001", ProblemSetMockData.TABLE_SET_00001_MYSQL_DDL, DbmsType.MYSQL);
        saveProblem("P00001-00002", "P00001", ProblemSetMockData.TABLE_SET_00001_POSTGRESQL_DDL, DbmsType.POSTGRESQL);
        saveProblem("M00001-00002", "M00001", ProblemSetMockData.TABLE_SET_00001_MYSQL_DDL, DbmsType.MYSQL);

        // 목록 확인용 PostgreSQL/MySQL 문제 Mock 데이터 추가 적재
        for (int sequence = 3; sequence <= 12; sequence++) {
            saveProblem(
                    "P00001-%05d".formatted(sequence), "P00001", "PostgreSQL 주문 집계 연습 %02d".formatted(sequence),
                    ProblemSetMockData.TABLE_SET_00001_POSTGRESQL_DDL, DbmsType.POSTGRESQL
            );
            saveProblem(
                    "M00001-%05d".formatted(sequence), "M00001", "MySQL 주문 집계 연습 %02d".formatted(sequence),
                    ProblemSetMockData.TABLE_SET_00001_MYSQL_DDL, DbmsType.MYSQL
            );
        }
    }

    private void saveProblem(String problemId, String problemSetId, String ddl, DbmsType dbmsType) {
        // 기본 제목으로 문제 저장
        saveProblem(problemId, problemSetId, "3월 고객별 주문 건수와 총 주문 금액 조회", ddl, dbmsType);
    }

    private void saveProblem(String problemId, String problemSetId, String title, String ddl, DbmsType dbmsType) {
        // 문제 저장
        problemRepository.save(Problem.create(
                problemId,
                problemSetId,
                title,
                DESCRIPTION,
                ddl,
                dbmsType,
                CONDITION,
                OUTPUT,
                SAMPLE_DATA_SQL,
                OUTPUT_SAMPLE,
                ANSWER,
                ANSWER_SQL
        ));
    }
}
