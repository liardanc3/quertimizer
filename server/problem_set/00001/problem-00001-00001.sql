INSERT INTO quertimizer.problem (problem_id,
                                 title,
                                 description,
                                 ddl_postgresql,
                                 ddl_oracle,
                                 condition,
                                 output,
                                 data_sample,
                                 output_sample,
                                 answer)
VALUES ('00001-00001',
        '3월 고객별 주문 건수와 총 주문 금액 조회',
        $description$
2024년 3월에 발생한 주문을 기준으로 고객별 주문 건수와 총 주문 금액을 조회해라.
$description$,
        $ddl_postgresql$
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
$ddl_postgresql$,
        $ddl_oracle$
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
$ddl_oracle$,
        $condition$
orders.ordered_at >= '2024-03-01 00:00:00'
orders.ordered_at < '2024-04-01 00:00:00'
고객별 주문 건수는 COUNT(DISTINCT orders.order_id)로 계산한다.
고객별 총 주문 금액은 SUM(order_items.quantity * order_items.unit_price)로 계산한다.
2024년 3월에 주문이 없는 고객은 결과에서 제외한다.
결과는 total_amount 내림차순, customer_id 오름차순으로 정렬한다.
$condition$,
        $output$
customer_id: 고객 ID
customer_name: 고객 이름
order_count: 2024년 3월 주문 건수
total_amount: 2024년 3월 총 주문 금액
$output$,
        $data_sample$
INSERT INTO customers (customer_id, customer_name, region, signup_date)
VALUES
    (1, '고객00001', 'BUSAN', '2023-01-18'),
    (2, '고객00002', 'DAEGU', '2023-02-04'),
    (3, '고객00003', 'INCHEON', '2023-02-21');

INSERT INTO orders (order_id, customer_id, ordered_at, order_status, payment_method)
VALUES
    (1101, 1, '2024-03-03 09:12:00', 'DELIVERED', 'CARD'),
    (1102, 1, '2024-03-18 14:26:00', 'DELIVERED', 'WALLET'),
    (1201, 2, '2024-03-07 11:05:00', 'PENDING', 'BANK_TRANSFER'),
    (1301, 3, '2024-04-02 16:40:00', 'DELIVERED', 'CARD');

INSERT INTO order_items (order_item_id, order_id, product_category, quantity, unit_price, discount_amount)
VALUES
    (1, 1101, 'LIVING', 2, 12000.00, 0.00),
    (2, 1101, 'FOOD', 1, 18000.00, 0.00),
    (3, 1102, 'DIGITAL', 1, 92000.00, 11040.00),
    (4, 1201, 'BEAUTY', 3, 26000.00, 0.00),
    (5, 1301, 'SPORTS', 1, 34000.00, 0.00);
$data_sample$,
        $output_sample$
customer_id,customer_name,order_count,total_amount
1,고객00001,2,134000.00
2,고객00002,1,78000.00
$output_sample$,
        $answer$
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
$answer$)
ON CONFLICT (problem_id) DO UPDATE
    SET title          = EXCLUDED.title,
        description    = EXCLUDED.description,
        ddl_postgresql = EXCLUDED.ddl_postgresql,
        ddl_oracle     = EXCLUDED.ddl_oracle,
        condition      = EXCLUDED.condition,
        output         = EXCLUDED.output,
        data_sample    = EXCLUDED.data_sample,
        output_sample  = EXCLUDED.output_sample,
        answer         = EXCLUDED.answer;


DELETE FROM quertimizer.problem_solve_history
WHERE problem_id = '00001-00001'
  AND user_id = 'liardanc3'
  AND dbms_type IN ('POSTGRESQL', 'ORACLE');

INSERT INTO quertimizer.problem_solve_history (
    problem_id,
    user_id,
    dbms_type,
    submitted_sql,
    execution_time_ms,
    scan_rows,
    execution_plan_element,
    submitted_at
)
VALUES
(
    '00001-00001',
    'liardanc3',
    'POSTGRESQL',
    $postgresql_sql$
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
    $postgresql_sql$,
    97,
    0,
    536954882,
    TIMESTAMP '2026-04-05 21:10:00'
),
(
    '00001-00001',
    'liardanc3',
    'ORACLE',
    $oracle_sql$
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
    $oracle_sql$,
    121,
    0,
    8523780,
    TIMESTAMP '2026-04-05 21:11:00'
);
