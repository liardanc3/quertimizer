TRUNCATE TABLE order_items, orders, customers;

INSERT INTO customers (customer_id, customer_name, region, signup_date)
SELECT
    gs,
    '고객' || LPAD(gs::text, 5, '0'),
    (ARRAY['SEOUL', 'BUSAN', 'DAEGU', 'INCHEON', 'GWANGJU'])[1 + (gs % 5)],
    DATE '2023-01-01' + ((((gs::bigint) * 17) % 540)::integer)
FROM generate_series(1, 15000) AS gs;

INSERT INTO orders (order_id, customer_id, ordered_at, order_status, payment_method)
SELECT
    gs,
    ((gs * 37) % 15000) + 1,
    TIMESTAMP '2024-01-01 00:00:00' + ((((gs::bigint) * 7919) % 15724800) * INTERVAL '1 second'),
    CASE
        WHEN gs % 10 < 7 THEN 'DELIVERED'
        WHEN gs % 10 < 9 THEN 'PENDING'
        ELSE 'CANCELLED'
    END,
    (ARRAY['CARD', 'BANK_TRANSFER', 'WALLET', 'POINT'])[1 + (gs % 4)]
FROM generate_series(1, 90000) AS gs;

INSERT INTO order_items (order_item_id, order_id, product_category, quantity, unit_price, discount_amount)
WITH expanded AS (
    SELECT
        o.order_id,
        line_no
    FROM orders o
    CROSS JOIN LATERAL generate_series(1, 2 + (o.order_id % 3)) AS line_no
)
SELECT
    ROW_NUMBER() OVER (ORDER BY order_id, line_no),
    order_id,
    (ARRAY['LIVING', 'FOOD', 'BEAUTY', 'DIGITAL', 'SPORTS', 'BOOK'])[1 + ((order_id + line_no) % 6)],
    CASE
        WHEN (order_id + line_no) % 11 = 0 THEN 5
        WHEN (order_id + line_no) % 5 = 0 THEN 3
        ELSE 1 + ((order_id + line_no) % 2)
    END,
    (ARRAY[12000.00, 18000.00, 26000.00, 34000.00, 47000.00, 68000.00, 92000.00])[1 + ((order_id + line_no) % 7)]::NUMERIC(12,2),
    ROUND(
        CASE
            WHEN (order_id + line_no) % 9 = 0
                THEN (ARRAY[12000.00, 18000.00, 26000.00, 34000.00, 47000.00, 68000.00, 92000.00])[1 + ((order_id + line_no) % 7)] * 0.12
            WHEN (order_id + line_no) % 4 = 0
                THEN (ARRAY[12000.00, 18000.00, 26000.00, 34000.00, 47000.00, 68000.00, 92000.00])[1 + ((order_id + line_no) % 7)] * 0.05
            ELSE 0
        END,
        2
    )
FROM expanded;
