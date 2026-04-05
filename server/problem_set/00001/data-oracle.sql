TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE customers;

INSERT INTO customers (customer_id, customer_name, region, signup_date)
SELECT
    LEVEL,
    '고객' || LPAD(TO_CHAR(LEVEL), 5, '0'),
    CASE MOD(LEVEL, 5)
        WHEN 0 THEN 'SEOUL'
        WHEN 1 THEN 'BUSAN'
        WHEN 2 THEN 'DAEGU'
        WHEN 3 THEN 'INCHEON'
        ELSE 'GWANGJU'
    END,
    DATE '2023-01-01' + MOD(LEVEL * 17, 540)
FROM dual
CONNECT BY LEVEL <= 15000;

INSERT INTO orders (order_id, customer_id, ordered_at, order_status, payment_method)
SELECT
    LEVEL,
    MOD(LEVEL * 37, 15000) + 1,
    TIMESTAMP '2024-01-01 00:00:00' + NUMTODSINTERVAL(MOD(LEVEL * 7919, 15724800), 'SECOND'),
    CASE
        WHEN MOD(LEVEL, 10) < 7 THEN 'DELIVERED'
        WHEN MOD(LEVEL, 10) < 9 THEN 'PENDING'
        ELSE 'CANCELLED'
    END,
    CASE MOD(LEVEL, 4)
        WHEN 0 THEN 'CARD'
        WHEN 1 THEN 'BANK_TRANSFER'
        WHEN 2 THEN 'WALLET'
        ELSE 'POINT'
    END
FROM dual
CONNECT BY LEVEL <= 90000;

INSERT INTO order_items (order_item_id, order_id, product_category, quantity, unit_price, discount_amount)
SELECT
    ROW_NUMBER() OVER (ORDER BY order_id, line_no),
    order_id,
    product_category,
    quantity,
    unit_price,
    discount_amount
FROM (
    SELECT
        o.order_id,
        l.line_no,
        CASE MOD(o.order_id + l.line_no, 6)
            WHEN 0 THEN 'LIVING'
            WHEN 1 THEN 'FOOD'
            WHEN 2 THEN 'BEAUTY'
            WHEN 3 THEN 'DIGITAL'
            WHEN 4 THEN 'SPORTS'
            ELSE 'BOOK'
        END AS product_category,
        CASE
            WHEN MOD(o.order_id + l.line_no, 11) = 0 THEN 5
            WHEN MOD(o.order_id + l.line_no, 5) = 0 THEN 3
            ELSE 1 + MOD(o.order_id + l.line_no, 2)
        END AS quantity,
        CASE MOD(o.order_id + l.line_no, 7)
            WHEN 0 THEN 12000
            WHEN 1 THEN 18000
            WHEN 2 THEN 26000
            WHEN 3 THEN 34000
            WHEN 4 THEN 47000
            WHEN 5 THEN 68000
            ELSE 92000
        END AS unit_price,
        ROUND(
            CASE
                WHEN MOD(o.order_id + l.line_no, 9) = 0 THEN
                    CASE MOD(o.order_id + l.line_no, 7)
                        WHEN 0 THEN 12000
                        WHEN 1 THEN 18000
                        WHEN 2 THEN 26000
                        WHEN 3 THEN 34000
                        WHEN 4 THEN 47000
                        WHEN 5 THEN 68000
                        ELSE 92000
                    END * 0.12
                WHEN MOD(o.order_id + l.line_no, 4) = 0 THEN
                    CASE MOD(o.order_id + l.line_no, 7)
                        WHEN 0 THEN 12000
                        WHEN 1 THEN 18000
                        WHEN 2 THEN 26000
                        WHEN 3 THEN 34000
                        WHEN 4 THEN 47000
                        WHEN 5 THEN 68000
                        ELSE 92000
                    END * 0.05
                ELSE 0
            END,
            2
        ) AS discount_amount
    FROM orders o
    JOIN (
        SELECT 1 AS line_no FROM dual
        UNION ALL
        SELECT 2 FROM dual
        UNION ALL
        SELECT 3 FROM dual
        UNION ALL
        SELECT 4 FROM dual
    ) l
        ON l.line_no <= 2 + MOD(o.order_id, 3)
);
