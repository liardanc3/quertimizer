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
