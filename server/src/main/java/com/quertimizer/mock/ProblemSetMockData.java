package com.quertimizer.mock;

import com.quertimizer.entity.ProblemSet;
import com.quertimizer.repository.ProblemSetRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("problemSetMockData")
@RequiredArgsConstructor
public class ProblemSetMockData {

    public static final String TABLE_SET_00001_POSTGRESQL_DDL = """
            CREATE TABLE customers (
                customer_id INTEGER PRIMARY KEY,
                customer_name VARCHAR(50) NOT NULL,
                region VARCHAR(30) NOT NULL,
                signup_date DATE NOT NULL
            );

            COMMENT ON TABLE customers IS '고객 기본 정보';
            COMMENT ON COLUMN customers.customer_id IS '고객 ID';
            COMMENT ON COLUMN customers.customer_name IS '고객 이름';
            COMMENT ON COLUMN customers.region IS '고객 지역';
            COMMENT ON COLUMN customers.signup_date IS '가입일';

            CREATE TABLE orders (
                order_id INTEGER PRIMARY KEY,
                customer_id INTEGER NOT NULL,
                ordered_at TIMESTAMP NOT NULL,
                order_status VARCHAR(20) NOT NULL,
                payment_method VARCHAR(30) NOT NULL,
                CONSTRAINT fk_orders_customers
                    FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
            );

            COMMENT ON TABLE orders IS '주문 기본 정보';
            COMMENT ON COLUMN orders.order_id IS '주문 ID';
            COMMENT ON COLUMN orders.customer_id IS '주문한 고객 ID';
            COMMENT ON COLUMN orders.ordered_at IS '주문 일시';
            COMMENT ON COLUMN orders.order_status IS '주문 상태';
            COMMENT ON COLUMN orders.payment_method IS '결제 수단';

            CREATE TABLE order_items (
                order_item_id INTEGER PRIMARY KEY,
                order_id INTEGER NOT NULL,
                product_category VARCHAR(30) NOT NULL,
                quantity INTEGER NOT NULL CHECK (quantity > 0),
                unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
                discount_amount NUMERIC(12,2) NOT NULL CHECK (discount_amount >= 0),
                CONSTRAINT fk_order_items_orders
                    FOREIGN KEY (order_id) REFERENCES orders (order_id)
            );

            COMMENT ON TABLE order_items IS '주문 상품 정보';
            COMMENT ON COLUMN order_items.order_item_id IS '주문 상품 ID';
            COMMENT ON COLUMN order_items.order_id IS '소속 주문 ID';
            COMMENT ON COLUMN order_items.product_category IS '상품 카테고리';
            COMMENT ON COLUMN order_items.quantity IS '구매 수량';
            COMMENT ON COLUMN order_items.unit_price IS '상품 단가';
            COMMENT ON COLUMN order_items.discount_amount IS '할인 금액';
            """;

    public static final String TABLE_SET_00001_ORACLE_DDL = """
            CREATE TABLE customers (
                customer_id NUMBER(10) PRIMARY KEY,
                customer_name VARCHAR2(50 CHAR) NOT NULL,
                region VARCHAR2(30 CHAR) NOT NULL,
                signup_date DATE NOT NULL
            );

            COMMENT ON TABLE customers IS '고객 기본 정보';
            COMMENT ON COLUMN customers.customer_id IS '고객 ID';
            COMMENT ON COLUMN customers.customer_name IS '고객 이름';
            COMMENT ON COLUMN customers.region IS '고객 지역';
            COMMENT ON COLUMN customers.signup_date IS '가입일';

            CREATE TABLE orders (
                order_id NUMBER(10) PRIMARY KEY,
                customer_id NUMBER(10) NOT NULL,
                ordered_at TIMESTAMP NOT NULL,
                order_status VARCHAR2(20 CHAR) NOT NULL,
                payment_method VARCHAR2(30 CHAR) NOT NULL,
                CONSTRAINT fk_orders_customers
                    FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
            );

            COMMENT ON TABLE orders IS '주문 기본 정보';
            COMMENT ON COLUMN orders.order_id IS '주문 ID';
            COMMENT ON COLUMN orders.customer_id IS '주문한 고객 ID';
            COMMENT ON COLUMN orders.ordered_at IS '주문 일시';
            COMMENT ON COLUMN orders.order_status IS '주문 상태';
            COMMENT ON COLUMN orders.payment_method IS '결제 수단';

            CREATE TABLE order_items (
                order_item_id NUMBER(10) PRIMARY KEY,
                order_id NUMBER(10) NOT NULL,
                product_category VARCHAR2(30 CHAR) NOT NULL,
                quantity NUMBER(10) NOT NULL CHECK (quantity > 0),
                unit_price NUMBER(12,2) NOT NULL CHECK (unit_price >= 0),
                discount_amount NUMBER(12,2) NOT NULL CHECK (discount_amount >= 0),
                CONSTRAINT fk_order_items_orders
                    FOREIGN KEY (order_id) REFERENCES orders (order_id)
            );

            COMMENT ON TABLE order_items IS '주문 상품 정보';
            COMMENT ON COLUMN order_items.order_item_id IS '주문 상품 ID';
            COMMENT ON COLUMN order_items.order_id IS '소속 주문 ID';
            COMMENT ON COLUMN order_items.product_category IS '상품 카테고리';
            COMMENT ON COLUMN order_items.quantity IS '구매 수량';
            COMMENT ON COLUMN order_items.unit_price IS '상품 단가';
            COMMENT ON COLUMN order_items.discount_amount IS '할인 금액';
            """;

    public static final String TABLE_SET_00001_POSTGRESQL_DATA = """
            INSERT INTO customers (customer_id, customer_name, region, signup_date)
            VALUES
                (1, '고객00001', 'BUSAN', DATE '2023-01-18'),
                (2, '고객00002', 'DAEGU', DATE '2023-02-04'),
                (3, '고객00003', 'INCHEON', DATE '2023-02-21');

            INSERT INTO orders (order_id, customer_id, ordered_at, order_status, payment_method)
            VALUES
                (1101, 1, TIMESTAMP '2024-03-03 09:12:00', 'DELIVERED', 'CARD'),
                (1102, 1, TIMESTAMP '2024-03-18 14:26:00', 'DELIVERED', 'WALLET'),
                (1201, 2, TIMESTAMP '2024-03-07 11:05:00', 'PENDING', 'BANK_TRANSFER'),
                (1301, 3, TIMESTAMP '2024-04-02 16:40:00', 'DELIVERED', 'CARD');

            INSERT INTO order_items (order_item_id, order_id, product_category, quantity, unit_price, discount_amount)
            VALUES
                (1, 1101, 'LIVING', 2, 12000.00, 0.00),
                (2, 1101, 'FOOD', 1, 18000.00, 0.00),
                (3, 1102, 'DIGITAL', 1, 92000.00, 11040.00),
                (4, 1201, 'BEAUTY', 3, 26000.00, 0.00),
                (5, 1301, 'SPORTS', 1, 34000.00, 0.00);
            """;

    public static final String TABLE_SET_00001_ORACLE_DATA = """
            INSERT INTO customers (customer_id, customer_name, region, signup_date)
            VALUES (1, '고객00001', 'BUSAN', DATE '2023-01-18');
            INSERT INTO customers (customer_id, customer_name, region, signup_date)
            VALUES (2, '고객00002', 'DAEGU', DATE '2023-02-04');
            INSERT INTO customers (customer_id, customer_name, region, signup_date)
            VALUES (3, '고객00003', 'INCHEON', DATE '2023-02-21');

            INSERT INTO orders (order_id, customer_id, ordered_at, order_status, payment_method)
            VALUES
                (1101, 1, TIMESTAMP '2024-03-03 09:12:00', 'DELIVERED', 'CARD');
            INSERT INTO orders (order_id, customer_id, ordered_at, order_status, payment_method)
            VALUES
                (1102, 1, TIMESTAMP '2024-03-18 14:26:00', 'DELIVERED', 'WALLET');
            INSERT INTO orders (order_id, customer_id, ordered_at, order_status, payment_method)
            VALUES
                (1201, 2, TIMESTAMP '2024-03-07 11:05:00', 'PENDING', 'BANK_TRANSFER');
            INSERT INTO orders (order_id, customer_id, ordered_at, order_status, payment_method)
            VALUES
                (1301, 3, TIMESTAMP '2024-04-02 16:40:00', 'DELIVERED', 'CARD');

            INSERT INTO order_items (order_item_id, order_id, product_category, quantity, unit_price, discount_amount)
            VALUES
                (1, 1101, 'LIVING', 2, 12000.00, 0.00);
            INSERT INTO order_items (order_item_id, order_id, product_category, quantity, unit_price, discount_amount)
            VALUES
                (2, 1101, 'FOOD', 1, 18000.00, 0.00);
            INSERT INTO order_items (order_item_id, order_id, product_category, quantity, unit_price, discount_amount)
            VALUES
                (3, 1102, 'DIGITAL', 1, 92000.00, 11040.00);
            INSERT INTO order_items (order_item_id, order_id, product_category, quantity, unit_price, discount_amount)
            VALUES
                (4, 1201, 'BEAUTY', 3, 26000.00, 0.00);
            INSERT INTO order_items (order_item_id, order_id, product_category, quantity, unit_price, discount_amount)
            VALUES
                (5, 1301, 'SPORTS', 1, 34000.00, 0.00);
            """;

    private final ProblemSetRepository problemSetRepository;

    @PostConstruct
    public void seed() {
        problemSetRepository.save(ProblemSet.create(
                "P00001",
                TABLE_SET_00001_POSTGRESQL_DDL,
                TABLE_SET_00001_POSTGRESQL_DATA,
                true,
                false
        ));
        problemSetRepository.save(ProblemSet.create(
                "O00001",
                TABLE_SET_00001_ORACLE_DDL,
                TABLE_SET_00001_ORACLE_DATA,
                false,
                true
        ));
    }
}
