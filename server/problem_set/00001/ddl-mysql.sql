CREATE TABLE customers (
    customer_id INT PRIMARY KEY COMMENT '고객 ID',
    customer_name VARCHAR(50) NOT NULL COMMENT '고객 이름',
    region VARCHAR(30) NOT NULL COMMENT '고객 지역',
    signup_date DATE NOT NULL COMMENT '가입일'
)
ENGINE=InnoDB
COMMENT='고객 기본 정보';

CREATE TABLE orders (
    order_id INT PRIMARY KEY COMMENT '주문 ID',
    customer_id INT NOT NULL COMMENT '주문한 고객 ID',
    ordered_at DATETIME NOT NULL COMMENT '주문 일시',
    order_status VARCHAR(20) NOT NULL COMMENT '주문 상태',
    payment_method VARCHAR(30) NOT NULL COMMENT '결제 수단',
    CONSTRAINT fk_orders_customers
        FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
)
ENGINE=InnoDB
COMMENT='주문 기본 정보';

CREATE TABLE order_items (
    order_item_id INT PRIMARY KEY COMMENT '주문 상품 ID',
    order_id INT NOT NULL COMMENT '소속 주문 ID',
    product_category VARCHAR(30) NOT NULL COMMENT '상품 카테고리',
    quantity INT NOT NULL COMMENT '구매 수량' CHECK (quantity > 0),
    unit_price DECIMAL(12,2) NOT NULL COMMENT '상품 단가' CHECK (unit_price >= 0),
    discount_amount DECIMAL(12,2) NOT NULL COMMENT '할인 금액' CHECK (discount_amount >= 0),
    CONSTRAINT fk_order_items_orders
        FOREIGN KEY (order_id) REFERENCES orders (order_id)
)
ENGINE=InnoDB
COMMENT='주문 상품 정보';
