import { mockFailResult, mockSuccessResult } from './results';
import type { ProblemDetail } from '../types/domain';

export const mockProblemDetails: ProblemDetail[] = [
  {
    id: 'p-101',
    number: 101,
    title: '월별 상위 3개 상품 매출 추출',
    preview: '주문 테이블과 상품 테이블을 조인하여 월별 매출 상위 상품을 찾으세요.',
    tags: ['SELECT', 'FROM', 'INNER_JOIN', 'DENSE_RANK'],
    difficulty: '중급',
    description:
      '2025년 주문 데이터에서 월별 총매출이 높은 상위 3개 상품을 구하세요. 같은 매출이면 같은 등수로 처리합니다.',
    schemaInfo:
      'orders(order_id, product_id, order_date, quantity, unit_price)\nproducts(product_id, product_name, category)',
    inputExample:
      'orders:\n(1, A12, 2025-11-02, 2, 12000)\n(2, B02, 2025-11-03, 5, 8000)',
    outputExample: 'month | product_id | total_sales\n2025-11 | A12 | 812000',
    starterSql:
      'SELECT\n  TO_CHAR(order_date, \'YYYY-MM\') AS month,\n  product_id,\n  SUM(quantity * unit_price) AS total_sales\nFROM orders\nGROUP BY TO_CHAR(order_date, \'YYYY-MM\'), product_id;',
    dbmsOptions: ['postgresql', 'oracle'],
    disabledDbms: ['oracle'],
    mockResult: mockSuccessResult,
  },
  {
    id: 'p-214',
    number: 214,
    title: '지점별 지연 주문 비율 계산',
    preview: '배송 완료 시간이 SLA를 넘긴 주문의 비율을 지점 단위로 계산하세요.',
    tags: ['CASE_WHEN', 'GROUP_BY', 'INDEX_HINT'],
    difficulty: '고급',
    description:
      '지점별 총 주문 수 대비 SLA 지연 주문 비율을 계산하고, 지연 비율이 높은 순으로 정렬하세요.',
    schemaInfo:
      'shipments(order_id, branch_id, promised_at, delivered_at)\nbranches(branch_id, branch_name)',
    inputExample:
      'shipments:\n(7001, S01, 2025-11-10 16:00, 2025-11-10 18:30)\n(7002, S01, 2025-11-10 18:00, 2025-11-10 17:59)',
    outputExample: 'branch_id | delayed_ratio\nS01 | 0.31',
    starterSql:
      'SELECT\n  s.branch_id,\n  AVG(CASE WHEN s.delivered_at > s.promised_at THEN 1.0 ELSE 0 END) AS delayed_ratio\nFROM shipments s\nGROUP BY s.branch_id;',
    dbmsOptions: ['postgresql', 'oracle'],
    disabledDbms: ['oracle'],
    mockResult: mockFailResult,
  },
  {
    id: 'p-305',
    number: 305,
    title: '최근 30일 재구매 고객 탐지',
    preview: '동일 고객의 재주문 패턴을 분석해 재구매 고객 목록을 작성하세요.',
    tags: ['WINDOW', 'LAG', 'DATE_DIFF'],
    difficulty: '중급',
    description:
      '최근 30일 내 동일 고객이 2회 이상 구매한 경우를 추출하고, 주문 간격 평균을 함께 구하세요.',
    schemaInfo:
      'customer_orders(order_id, customer_id, order_at, amount)',
    inputExample:
      'customer_orders:\n(1, C001, 2025-11-01, 43000)\n(2, C001, 2025-11-13, 62000)',
    outputExample: 'customer_id | repurchase_count | avg_days_gap\nC001 | 3 | 8.5',
    starterSql:
      'SELECT\n  customer_id,\n  COUNT(*) AS repurchase_count\nFROM customer_orders\nWHERE order_at >= CURRENT_DATE - INTERVAL \'30 days\'\nGROUP BY customer_id;',
    dbmsOptions: ['postgresql', 'oracle'],
    disabledDbms: ['oracle'],
    mockResult: mockSuccessResult,
  },
];

export const mockProblemDetailById = Object.fromEntries(
  mockProblemDetails.map((problem) => [problem.id, problem])
);
