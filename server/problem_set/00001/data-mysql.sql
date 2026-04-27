TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE customers;

INSERT INTO customers (customer_id, customer_name, region, signup_date)
VALUES
    (1, '고객00001', 'BUSAN', '2023-01-18'),
    (2, '고객00002', 'DAEGU', '2023-02-04'),
    (3, '고객00003', 'INCHEON', '2023-02-21'),
    (4, '고객00004', 'SEOUL', '2023-03-10'),
    (5, '고객00005', 'GWANGJU', '2023-03-27');

INSERT INTO orders (order_id, customer_id, ordered_at, order_status, payment_method)
VALUES
    (1101, 1, '2024-03-03 09:12:00', 'DELIVERED', 'CARD'),
    (1102, 1, '2024-03-18 14:26:00', 'DELIVERED', 'WALLET'),
    (1201, 2, '2024-03-07 11:05:00', 'PENDING', 'BANK_TRANSFER'),
    (1301, 3, '2024-04-02 16:40:00', 'DELIVERED', 'CARD'),
    (1401, 4, '2024-03-12 10:30:00', 'DELIVERED', 'POINT'),
    (1501, 5, '2024-03-22 18:05:00', 'CANCELLED', 'CARD');

INSERT INTO order_items (order_item_id, order_id, product_category, quantity, unit_price, discount_amount)
VALUES
    (1, 1101, 'LIVING', 2, 12000.00, 0.00),
    (2, 1101, 'FOOD', 1, 18000.00, 0.00),
    (3, 1102, 'DIGITAL', 1, 92000.00, 11040.00),
    (4, 1201, 'BEAUTY', 3, 26000.00, 0.00),
    (5, 1301, 'SPORTS', 1, 34000.00, 0.00),
    (6, 1401, 'BOOK', 2, 18000.00, 1800.00),
    (7, 1501, 'FOOD', 1, 26000.00, 0.00);
