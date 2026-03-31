import { mockFailResult, mockSuccessResult } from './results';
import type {
  AggregateBucket,
  DbmsType,
  FilterBucket,
  JoinBucket,
  ProblemDetail,
  RuntimeBucket,
  RuntimeDistribution,
  RuntimeSample,
  ScanBucket,
  SortBucket,
} from '../types/domain';

const DIFFICULTY_BEGINNER: ProblemDetail['difficulty'] = '입문';
const DIFFICULTY_INTERMEDIATE: ProblemDetail['difficulty'] = '중급';
const DIFFICULTY_ADVANCED: ProblemDetail['difficulty'] = '고급';

const DEFAULT_DBMS_OPTIONS: DbmsType[] = ['postgresql', 'oracle'];

interface SampleSeed {
  nickname: string;
  timeMs: number;
  rowsScanned: number;
  indexUsed: boolean;
  fullScan: boolean;
  hintUsed: boolean;
  scanBucket: ScanBucket;
  joinBucket: JoinBucket;
  filterBucket: FilterBucket;
  sortBucket: SortBucket;
  aggregateBucket: AggregateBucket;
  submittedOffsetHours: number;
  isMine?: boolean;
}

interface ProblemSeed {
  id: string;
  number: number;
  title: string;
  preview: string;
  description: string;
  tags: string[];
  difficulty: ProblemDetail['difficulty'];
  solvedCount: number;
  solvedAt?: string;
  schemaInfo: string;
  inputExample: string;
  outputExample: string;
  starterSql: string;
  disabledDbms?: DbmsType[];
  success: boolean;
  resultTimeMs: number;
  resultScanRows: number;
  resultCost: number;
  resultIndexUsed: boolean;
  resultFullScan: boolean;
  resultRows: string[][];
  postgresqlSamples: SampleSeed[];
  oracleSamples: SampleSeed[];
}

function toIsoLabel(offsetHours: number) {
  const baseDate = new Date('2026-03-31T09:00:00+09:00');
  baseDate.setHours(baseDate.getHours() - offsetHours);
  return baseDate.toISOString().slice(0, 16).replace('T', ' ');
}

function createBuckets(samples: RuntimeSample[], bucketSizeMs: number): RuntimeBucket[] {
  if (samples.length === 0) {
    return [];
  }

  const minValue = Math.min(...samples.map((sample) => sample.timeMs));
  const maxValue = Math.max(...samples.map((sample) => sample.timeMs));
  const start = Math.floor(minValue / bucketSizeMs) * bucketSizeMs;
  const end = Math.ceil(maxValue / bucketSizeMs) * bucketSizeMs;
  const buckets: RuntimeBucket[] = [];

  for (let value = start; value <= end; value += bucketSizeMs) {
    const count = samples.filter((sample) => sample.timeMs >= value && sample.timeMs < value + bucketSizeMs).length;
    buckets.push({ startMs: value, count });
  }

  return buckets;
}

function createDistribution(samples: SampleSeed[], bucketSizeMs: number): RuntimeDistribution {
  const runtimeSamples: RuntimeSample[] = samples.map((sample) => ({
    nickname: sample.nickname,
    timeMs: sample.timeMs,
    rowsScanned: sample.rowsScanned,
    submittedAt: toIsoLabel(sample.submittedOffsetHours),
    indexUsed: sample.indexUsed,
    fullScan: sample.fullScan,
    hintUsed: sample.hintUsed,
    scanBucket: sample.scanBucket,
    joinBucket: sample.joinBucket,
    filterBucket: sample.filterBucket,
    sortBucket: sample.sortBucket,
    aggregateBucket: sample.aggregateBucket,
    isMine: sample.isMine,
  }));
  const sortedByTime = [...runtimeSamples].sort((left, right) => left.timeMs - right.timeMs);
  const timeValues = sortedByTime.map((sample) => sample.timeMs);
  const averageTimeMs = timeValues.reduce((sum, value) => sum + value, 0) / timeValues.length;
  const medianTimeMs = timeValues[Math.floor(timeValues.length / 2)];
  const varianceMs =
    timeValues.reduce((sum, value) => sum + (value - averageTimeMs) * (value - averageTimeMs), 0) / timeValues.length;
  const mySample = runtimeSamples.find((sample) => sample.isMine);
  const indexUsageRate = (runtimeSamples.filter((sample) => sample.indexUsed).length / runtimeSamples.length) * 100;
  const fullScanRate = (runtimeSamples.filter((sample) => sample.fullScan).length / runtimeSamples.length) * 100;
  const averageRowsScanned =
    runtimeSamples.reduce((sum, sample) => sum + sample.rowsScanned, 0) / runtimeSamples.length;
  const hintUsageRate = (runtimeSamples.filter((sample) => sample.hintUsed).length / runtimeSamples.length) * 100;

  return {
    bucketSizeMs,
    buckets: createBuckets(runtimeSamples, bucketSizeMs),
    fastestTimeMs: sortedByTime[0].timeMs,
    fastestNickname: sortedByTime[0].nickname,
    fastestSolvedAt: sortedByTime[0].submittedAt,
    averageTimeMs: Math.round(averageTimeMs * 10) / 10,
    medianTimeMs,
    standardDeviationMs: Math.round(Math.sqrt(varianceMs) * 10) / 10,
    varianceMs: Math.round(varianceMs * 10) / 10,
    myTimeMs: mySample?.timeMs,
    submissionCount: runtimeSamples.length,
    topPerformers: sortedByTime.slice(0, 5).map((sample) => ({
      nickname: sample.nickname,
      timeMs: sample.timeMs,
      submittedAt: sample.submittedAt,
    })),
    samples: runtimeSamples,
    tuningStats: {
      p90TimeMs: sortedByTime[Math.max(0, Math.floor(sortedByTime.length * 0.9) - 1)].timeMs,
      indexUsageRate: Math.round(indexUsageRate * 10) / 10,
      fullScanRate: Math.round(fullScanRate * 10) / 10,
      averageRowsScanned: Math.round(averageRowsScanned),
      bufferHitRate: Math.round((95 - fullScanRate * 0.22) * 10) / 10,
      tempSpillRate: Math.round((8 + fullScanRate * 0.18) * 10) / 10,
      hintUsageRate: Math.round(hintUsageRate * 10) / 10,
    },
  };
}

function createProblem(seed: ProblemSeed): ProblemDetail {
  const runtimeDistributions = {
    postgresql: createDistribution(seed.postgresqlSamples, 5),
    oracle: createDistribution(seed.oracleSamples, 5),
  };

  return {
    id: seed.id,
    domain: 'rdbms',
    number: seed.number,
    title: seed.title,
    preview: seed.preview,
    tags: seed.tags,
    difficulty: seed.difficulty,
    solvedCount: seed.solvedCount,
    solvedAt: seed.solvedAt,
    runtimeDistribution: runtimeDistributions.postgresql,
    runtimeDistributions,
    description: seed.description,
    schemaInfo: seed.schemaInfo,
    inputExample: seed.inputExample,
    outputExample: seed.outputExample,
    starterSql: seed.starterSql,
    dbmsOptions: DEFAULT_DBMS_OPTIONS,
    disabledDbms: seed.disabledDbms ?? [],
    mockResult: {
      ...(seed.success ? mockSuccessResult : mockFailResult),
      executionTimeMs: seed.resultTimeMs,
      scanRows: seed.resultScanRows,
      cost: seed.resultCost,
      indexUsed: seed.resultIndexUsed,
      fullScan: seed.resultFullScan,
      rows: seed.resultRows.map((columns) => ({ columns })),
    },
  };
}

const problemSeeds: ProblemSeed[] = [
  {
    id: 'p-101',
    number: 101,
    title: 'VIP 고객 세그먼트 점수화',
    preview: '구매 금액과 최근 방문 이력을 함께 집계해 리텐션 캠페인 대상 VIP 고객을 찾아보세요.',
    description:
      '월별 주문 금액, 평균 객단가, 마지막 방문 시점을 결합한 고객 점수를 계산하세요. 최종 결과에는 캠페인 기준 점수를 넘는 고객만 포함되어야 합니다.',
    tags: ['윈도우', '집계', '캠페인'],
    difficulty: DIFFICULTY_INTERMEDIATE,
    solvedCount: 2841,
    solvedAt: '2026-03-28 21:12',
    schemaInfo: 'customers(customer_id, tier, joined_at)\norders(order_id, customer_id, order_amount, ordered_at)',
    inputExample: 'customers: 4행\norders: 11행',
    outputExample: 'customer_id | score | latest_visit_at',
    starterSql:
      'SELECT o.customer_id,\n       SUM(o.order_amount) AS score,\n       MAX(o.ordered_at) AS latest_visit_at\nFROM orders o\nGROUP BY o.customer_id;',
    success: true,
    resultTimeMs: 18.6,
    resultScanRows: 11240,
    resultCost: 57,
    resultIndexUsed: true,
    resultFullScan: false,
    resultRows: [
      ['1024', '98200', '2026-03-01 10:15'],
      ['2031', '87440', '2026-02-28 18:30'],
      ['3102', '85100', '2026-02-27 09:45'],
    ],
    postgresqlSamples: [
      { nickname: 'quertimizer_me', timeMs: 18.6, rowsScanned: 11240, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 12, isMine: true },
      { nickname: 'indexnova', timeMs: 15.1, rowsScanned: 9050, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 25 },
      { nickname: 'joinpilot', timeMs: 16.2, rowsScanned: 9780, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'MERGE_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'PLAIN_SORT', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 31 },
      { nickname: 'planforge', timeMs: 19.4, rowsScanned: 12820, indexUsed: true, fullScan: false, hintUsed: true, scanBucket: 'INDEX_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'POST_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 44 },
      { nickname: 'latencylab', timeMs: 21.3, rowsScanned: 14400, indexUsed: false, fullScan: true, hintUsed: false, scanBucket: 'FULL_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'POST_FILTER', sortBucket: 'PLAIN_SORT', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 49 },
      { nickname: 'coveridx', timeMs: 17.5, rowsScanned: 10330, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 55 },
    ],
    oracleSamples: [
      { nickname: 'quertimizer_me', timeMs: 20.1, rowsScanned: 11980, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'ROWID_ACCESS', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'GROUP_SORT', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 13, isMine: true },
      { nickname: 'indexnova', timeMs: 17.9, rowsScanned: 9500, indexUsed: true, fullScan: false, hintUsed: true, scanBucket: 'ROWID_ACCESS', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 29 },
      { nickname: 'joinpilot', timeMs: 18.7, rowsScanned: 10020, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'MERGE_JOIN', filterBucket: 'POST_FILTER', sortBucket: 'ORDER_SORT', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 36 },
      { nickname: 'planforge', timeMs: 22.8, rowsScanned: 15100, indexUsed: false, fullScan: true, hintUsed: false, scanBucket: 'FULL_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'POST_FILTER', sortBucket: 'GROUP_SORT', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 46 },
      { nickname: 'costwave', timeMs: 19.5, rowsScanned: 11040, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'ROWID_ACCESS', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 58 },
      { nickname: 'vectorkim', timeMs: 18.4, rowsScanned: 10220, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'NESTED_LOOP', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'PLAIN_AGG', submittedOffsetHours: 63 },
    ],
  },
  {
    id: 'p-214',
    number: 214,
    title: '월별 상위 3개 상품 매출',
    preview: '매출 팩트 테이블을 과하게 스캔하지 않으면서 월별 베스트 상품을 찾아보세요.',
    description:
      '확정 매출 금액을 기준으로 월별 상위 3개 상품을 반환하세요. 동점이면 더 이른 상품 ID가 먼저 오도록 정렬해야 합니다.',
    tags: ['랭킹', '윈도우', '매출'],
    difficulty: DIFFICULTY_ADVANCED,
    solvedCount: 1934,
    schemaInfo: 'sales(sale_id, sold_month, product_id, net_amount, status)\nproducts(product_id, category, seller_id)',
    inputExample: 'sales: 120만 행\nproducts: 4.2만 행',
    outputExample: 'sold_month | product_id | rank_no | net_amount',
    starterSql:
      'SELECT sold_month,\n       product_id,\n       SUM(net_amount) AS net_amount\nFROM sales\nWHERE status = \'CONFIRMED\'\nGROUP BY sold_month, product_id;',
    success: false,
    resultTimeMs: 44.8,
    resultScanRows: 58420,
    resultCost: 411,
    resultIndexUsed: false,
    resultFullScan: true,
    resultRows: [
      ['2026-01', 'A-220', '1', '921000'],
      ['2026-01', 'B-019', '2', '884000'],
    ],
    postgresqlSamples: [
      { nickname: 'fastjoin', timeMs: 26.5, rowsScanned: 28840, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'MERGE_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'INCREMENTAL_SORT', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 16 },
      { nickname: 'subquerycat', timeMs: 29.4, rowsScanned: 32450, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'DERIVED_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'POST_FILTER', sortBucket: 'PLAIN_SORT', aggregateBucket: 'WINDOW_AGG', submittedOffsetHours: 28 },
      { nickname: 'quertimizer_me', timeMs: 32.7, rowsScanned: 35200, indexUsed: true, fullScan: false, hintUsed: true, scanBucket: 'INDEX_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'POST_FILTER', sortBucket: 'PLAIN_SORT', aggregateBucket: 'WINDOW_AGG', submittedOffsetHours: 39, isMine: true },
      { nickname: 'costwave', timeMs: 35.8, rowsScanned: 40110, indexUsed: false, fullScan: true, hintUsed: false, scanBucket: 'FULL_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'POST_FILTER', sortBucket: 'PLAIN_SORT', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 53 },
      { nickname: 'rowfinder', timeMs: 31.2, rowsScanned: 34080, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'BITMAP_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'INCREMENTAL_SORT', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 70 },
    ],
    oracleSamples: [
      { nickname: 'fastjoin', timeMs: 28.1, rowsScanned: 29510, indexUsed: true, fullScan: false, hintUsed: true, scanBucket: 'BITMAP_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'ORDER_SORT', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 18 },
      { nickname: 'subquerycat', timeMs: 31.9, rowsScanned: 33620, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'DERIVED_SCAN', joinBucket: 'MERGE_JOIN', filterBucket: 'POST_FILTER', sortBucket: 'GROUP_SORT', aggregateBucket: 'WINDOW_AGG', submittedOffsetHours: 34 },
      { nickname: 'quertimizer_me', timeMs: 34.2, rowsScanned: 36180, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'ROWID_ACCESS', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'ORDER_SORT', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 41, isMine: true },
      { nickname: 'costwave', timeMs: 38.7, rowsScanned: 43000, indexUsed: false, fullScan: true, hintUsed: false, scanBucket: 'FULL_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'POST_FILTER', sortBucket: 'ORDER_SORT', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 57 },
      { nickname: 'rowfinder', timeMs: 30.4, rowsScanned: 32110, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'NESTED_LOOP', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'PLAIN_AGG', submittedOffsetHours: 69 },
    ],
  },
  {
    id: 'p-305',
    number: 305,
    title: '지점별 SLA 지연 비율',
    preview: '상태 필터의 선택도를 유지하면서 지점별 지연 티켓 비율을 계산해보세요.',
    description:
      '대상 분기의 완료 티켓만 사용해 지점별 지연 비율을 계산하세요. 유효 티켓이 10건 미만인 지점은 결과에서 제외해야 합니다.',
    tags: ['CASE', '조건부집계', '지원'],
    difficulty: DIFFICULTY_INTERMEDIATE,
    solvedCount: 2288,
    solvedAt: '2026-03-27 10:20',
    schemaInfo: 'tickets(ticket_id, branch_id, opened_at, closed_at, sla_minutes, status)\nbranches(branch_id, region, opened_date)',
    inputExample: 'tickets: 42만 행\nbranches: 460행',
    outputExample: 'branch_id | delay_ratio | closed_count',
    starterSql:
      'SELECT t.branch_id,\n       AVG(CASE WHEN t.closed_at > t.opened_at + (t.sla_minutes || \' minutes\')::interval THEN 1 ELSE 0 END) AS delay_ratio\nFROM tickets t\nGROUP BY t.branch_id;',
    success: true,
    resultTimeMs: 23.3,
    resultScanRows: 18200,
    resultCost: 88,
    resultIndexUsed: true,
    resultFullScan: false,
    resultRows: [
      ['B-17', '0.182', '112'],
      ['B-24', '0.164', '98'],
      ['B-32', '0.155', '121'],
    ],
    postgresqlSamples: [
      { nickname: 'aggnerd', timeMs: 18.4, rowsScanned: 14220, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'NONE', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 11 },
      { nickname: 'quertimizer_me', timeMs: 23.3, rowsScanned: 18200, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'NONE', filterBucket: 'POST_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 26, isMine: true },
      { nickname: 'bitmapfox', timeMs: 20.1, rowsScanned: 16110, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'BITMAP_SCAN', joinBucket: 'NONE', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 34 },
      { nickname: 'filterlab', timeMs: 24.5, rowsScanned: 19600, indexUsed: false, fullScan: true, hintUsed: false, scanBucket: 'FULL_SCAN', joinBucket: 'NONE', filterBucket: 'POST_FILTER', sortBucket: 'NONE', aggregateBucket: 'PLAIN_AGG', submittedOffsetHours: 43 },
    ],
    oracleSamples: [
      { nickname: 'aggnerd', timeMs: 19.7, rowsScanned: 15080, indexUsed: true, fullScan: false, hintUsed: true, scanBucket: 'ROWID_ACCESS', joinBucket: 'NONE', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 15 },
      { nickname: 'quertimizer_me', timeMs: 24.6, rowsScanned: 18840, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'ROWID_ACCESS', joinBucket: 'NONE', filterBucket: 'POST_FILTER', sortBucket: 'GROUP_SORT', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 22, isMine: true },
      { nickname: 'bitmapfox', timeMs: 21.8, rowsScanned: 17020, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'BITMAP_SCAN', joinBucket: 'NONE', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 40 },
      { nickname: 'filterlab', timeMs: 27.3, rowsScanned: 20800, indexUsed: false, fullScan: true, hintUsed: false, scanBucket: 'FULL_SCAN', joinBucket: 'NONE', filterBucket: 'POST_FILTER', sortBucket: 'GROUP_SORT', aggregateBucket: 'PLAIN_AGG', submittedOffsetHours: 52 },
    ],
  },
  {
    id: 'p-417',
    number: 417,
    title: '휴면 고객 리텐션 쿠폰 대상',
    preview: '최근 구매가 없는 고객을 찾아 예상 반응 가치 순으로 정렬해보세요.',
    description:
      '최근 45일 동안 주문이 없는 고객을 추려 예상 쿠폰 가치 점수를 계산하고, 세그먼트별 상위 후보를 반환하세요.',
    tags: ['CTE', '세그먼트', '리텐션'],
    difficulty: DIFFICULTY_BEGINNER,
    solvedCount: 3510,
    schemaInfo: 'members(member_id, segment, joined_at)\nmember_orders(order_id, member_id, ordered_at, order_amount)',
    inputExample: 'members: 8.2만 행\nmember_orders: 380만 행',
    outputExample: 'segment | member_id | expected_coupon_value',
    starterSql:
      'WITH latest_order AS (\n  SELECT member_id, MAX(ordered_at) AS last_order_at\n  FROM member_orders\n  GROUP BY member_id\n)\nSELECT *\nFROM latest_order;',
    success: true,
    resultTimeMs: 16.9,
    resultScanRows: 9800,
    resultCost: 49,
    resultIndexUsed: true,
    resultFullScan: false,
    resultRows: [
      ['gold', '5501', '18000'],
      ['silver', '8890', '16000'],
      ['bronze', '9012', '12000'],
    ],
    postgresqlSamples: [
      { nickname: 'retentor', timeMs: 13.8, rowsScanned: 8410, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 8 },
      { nickname: 'quertimizer_me', timeMs: 16.9, rowsScanned: 9800, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 19, isMine: true },
      { nickname: 'couponcat', timeMs: 17.5, rowsScanned: 10220, indexUsed: true, fullScan: false, hintUsed: true, scanBucket: 'INDEX_SCAN', joinBucket: 'NESTED_LOOP', filterBucket: 'POST_FILTER', sortBucket: 'NONE', aggregateBucket: 'PLAIN_AGG', submittedOffsetHours: 37 },
      { nickname: 'seggraph', timeMs: 20.2, rowsScanned: 12110, indexUsed: false, fullScan: true, hintUsed: false, scanBucket: 'FULL_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'POST_FILTER', sortBucket: 'NONE', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 48 },
    ],
    oracleSamples: [
      { nickname: 'retentor', timeMs: 14.9, rowsScanned: 8900, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'ROWID_ACCESS', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 14 },
      { nickname: 'quertimizer_me', timeMs: 17.8, rowsScanned: 10140, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'ROWID_ACCESS', joinBucket: 'HASH_JOIN', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 24, isMine: true },
      { nickname: 'couponcat', timeMs: 18.6, rowsScanned: 10820, indexUsed: true, fullScan: false, hintUsed: true, scanBucket: 'INDEX_SCAN', joinBucket: 'NESTED_LOOP', filterBucket: 'POST_FILTER', sortBucket: 'NONE', aggregateBucket: 'PLAIN_AGG', submittedOffsetHours: 33 },
      { nickname: 'seggraph', timeMs: 21.5, rowsScanned: 12600, indexUsed: false, fullScan: true, hintUsed: false, scanBucket: 'FULL_SCAN', joinBucket: 'HASH_JOIN', filterBucket: 'POST_FILTER', sortBucket: 'GROUP_SORT', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 45 },
    ],
  },
  {
    id: 'p-522',
    number: 522,
    title: '결제 수단별 승인 성공률',
    preview: '간결한 조건부 집계로 결제 수단별 승인 성공률을 비교해보세요.',
    description:
      '선택한 주간 기준으로 결제 수단별 시도 건수, 승인 건수, 승인 비율을 함께 반환하세요.',
    tags: ['승인', '결제', '조건부집계'],
    difficulty: DIFFICULTY_BEGINNER,
    solvedCount: 4012,
    schemaInfo: 'payments(payment_id, method, status, requested_at, amount)',
    inputExample: 'payments: 97만 행',
    outputExample: 'method | attempts | approved_count | approval_rate',
    starterSql:
      'SELECT method,\n       COUNT(*) AS attempts,\n       SUM(CASE WHEN status = \'APPROVED\' THEN 1 ELSE 0 END) AS approved_count\nFROM payments\nGROUP BY method;',
    success: true,
    resultTimeMs: 11.4,
    resultScanRows: 7420,
    resultCost: 33,
    resultIndexUsed: true,
    resultFullScan: false,
    resultRows: [
      ['card', '4200', '3964', '94.4'],
      ['bank', '3120', '2868', '91.9'],
      ['wallet', '1290', '1228', '95.2'],
    ],
    postgresqlSamples: [
      { nickname: 'paychart', timeMs: 9.8, rowsScanned: 6100, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'NONE', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 10 },
      { nickname: 'quertimizer_me', timeMs: 11.4, rowsScanned: 7420, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'INDEX_SCAN', joinBucket: 'NONE', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'HASH_AGG', submittedOffsetHours: 20, isMine: true },
      { nickname: 'cardwave', timeMs: 12.2, rowsScanned: 8010, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'BITMAP_SCAN', joinBucket: 'NONE', filterBucket: 'POST_FILTER', sortBucket: 'NONE', aggregateBucket: 'PLAIN_AGG', submittedOffsetHours: 41 },
      { nickname: 'retrylane', timeMs: 14.1, rowsScanned: 9800, indexUsed: false, fullScan: true, hintUsed: false, scanBucket: 'FULL_SCAN', joinBucket: 'NONE', filterBucket: 'POST_FILTER', sortBucket: 'NONE', aggregateBucket: 'PLAIN_AGG', submittedOffsetHours: 60 },
    ],
    oracleSamples: [
      { nickname: 'paychart', timeMs: 10.3, rowsScanned: 6550, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'ROWID_ACCESS', joinBucket: 'NONE', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 12 },
      { nickname: 'quertimizer_me', timeMs: 12.1, rowsScanned: 7700, indexUsed: true, fullScan: false, hintUsed: false, scanBucket: 'ROWID_ACCESS', joinBucket: 'NONE', filterBucket: 'ACCESS_FILTER', sortBucket: 'NONE', aggregateBucket: 'GROUP_AGG', submittedOffsetHours: 21, isMine: true },
      { nickname: 'cardwave', timeMs: 12.9, rowsScanned: 8220, indexUsed: true, fullScan: false, hintUsed: true, scanBucket: 'BITMAP_SCAN', joinBucket: 'NONE', filterBucket: 'POST_FILTER', sortBucket: 'NONE', aggregateBucket: 'PLAIN_AGG', submittedOffsetHours: 38 },
      { nickname: 'retrylane', timeMs: 15.4, rowsScanned: 10280, indexUsed: false, fullScan: true, hintUsed: false, scanBucket: 'FULL_SCAN', joinBucket: 'NONE', filterBucket: 'POST_FILTER', sortBucket: 'ORDER_SORT', aggregateBucket: 'PLAIN_AGG', submittedOffsetHours: 64 },
    ],
  },
];

export const mockProblemDetails: ProblemDetail[] = problemSeeds.map(createProblem);

export const mockProblemDetailById: Record<string, ProblemDetail> = Object.fromEntries(
  mockProblemDetails.map((problem) => [problem.id, problem])
);
