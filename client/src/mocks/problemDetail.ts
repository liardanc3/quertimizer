import { mockFailResult, mockSuccessResult } from './results';
import type {
  DbmsType,
  MockResult,
  ProblemDetail,
  RuntimeDistribution,
  RuntimeDistributionByDbms,
  RuntimeSample,
} from '../types/domain';

interface MockSolverRun extends RuntimeSample {
  bufferHitRate: number;
  tempSpill: boolean;
}

interface RuntimeMockOptions {
  dbms?: DbmsType;
  seed: number;
  count: number;
  fastestSolvedAt: string;
  fastestRangeMs: [number, number];
  slowestRangeMs: [number, number];
  myTimeMs?: number;
  meanBias?: number;
  spreadFactor?: number;
}

interface MockSolverRunResult {
  runs: MockSolverRun[];
  slowestTimeMs: number;
  myTimeMs?: number;
}

interface ProblemTemplate {
  id: string;
  number: number;
  title: string;
  preview: string;
  tags: string[];
  difficulty: ProblemDetail['difficulty'];
  solvedAt?: string;
  runtimeOptions?: Omit<RuntimeMockOptions, 'count'>;
  description: string;
  schemaInfo: string;
  inputExample: string;
  outputExample: string;
  starterSql: string;
  mockResult: MockResult;
}

function createSeededRandom(seed: number) {
  let state = seed % 2147483647;

  if (state <= 0) {
    state += 2147483646;
  }

  return () => {
    state = (state * 16807) % 2147483647;
    return (state - 1) / 2147483646;
  };
}

function nextGaussian(random: () => number) {
  const u1 = Math.max(random(), Number.EPSILON);
  const u2 = Math.max(random(), Number.EPSILON);

  return Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function randomInt(random: () => number, min: number, max: number) {
  return min + Math.floor(random() * (max - min + 1));
}

function pickWeightedValue<T extends string>(random: () => number, entries: Array<[T, number]>) {
  const totalWeight = entries.reduce((sum, [, weight]) => sum + weight, 0);
  let threshold = random() * totalWeight;

  for (const [value, weight] of entries) {
    threshold -= weight;
    if (threshold <= 0) {
      return value;
    }
  }

  return entries[entries.length - 1][0];
}

function floorToStep(value: number, step: number) {
  return Math.floor(value / step) * step;
}

function ceilToStep(value: number, step: number) {
  return Math.ceil(value / step) * step;
}

function formatDateTime(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  const hour = String(value.getHours()).padStart(2, '0');
  const minute = String(value.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function offsetDateTime(baseDateTime: string, offsetMinutes: number) {
  const date = new Date(baseDateTime.replace(' ', 'T') + ':00');
  date.setMinutes(date.getMinutes() + offsetMinutes);
  return formatDateTime(date);
}

function createNickname(index: number, random: () => number) {
  const prefixes = ['index', 'join', 'window', 'group', 'plan', 'query', 'hash', 'merge', 'scan', 'table'];
  const suffixes = ['runner', 'smith', 'pilot', 'builder', 'master', 'tuner', 'wave', 'spark', 'finder', 'forge'];
  const badge = 100 + Math.floor(random() * 900);

  return `${prefixes[index % prefixes.length]}_${suffixes[Math.floor(random() * suffixes.length)]}${badge}`;
}

function createPlanBuckets(args: {
  dbms: DbmsType;
  fullScan: boolean;
  indexUsed: boolean;
  hintUsed: boolean;
  rowsScanned: number;
  timePosition: number;
  random: () => number;
}) {
  const { dbms, fullScan, indexUsed, hintUsed, rowsScanned, timePosition, random } = args;
  const scanBucket: RuntimeSample['scanBucket'] =
    dbms === 'oracle'
      ? fullScan
        ? pickWeightedValue(random, [
            ['FULL_SCAN', 0.64],
            ['DERIVED_SCAN', 0.16],
            ['REMOTE_SCAN', 0.08],
            ['BITMAP_SCAN', 0.06],
            ['OTHERS', 0.06],
          ])
        : indexUsed
          ? pickWeightedValue(random, [
              ['INDEX_SCAN', 0.48],
              ['ROWID_ACCESS', 0.3],
              ['BITMAP_SCAN', 0.12],
              ['DERIVED_SCAN', 0.05],
              ['OTHERS', 0.05],
            ])
          : pickWeightedValue(random, [
              ['DERIVED_SCAN', 0.36],
              ['FULL_SCAN', 0.22],
              ['REMOTE_SCAN', 0.12],
              ['INDEX_SCAN', 0.12],
              ['ROWID_ACCESS', 0.08],
              ['BITMAP_SCAN', 0.05],
              ['OTHERS', 0.05],
            ])
      : fullScan
        ? pickWeightedValue(random, [
            ['FULL_SCAN', 0.64],
            ['DERIVED_SCAN', 0.18],
            ['BITMAP_SCAN', 0.1],
            ['OTHERS', 0.08],
          ])
        : indexUsed
          ? pickWeightedValue(random, [
              ['INDEX_SCAN', 0.72],
              ['BITMAP_SCAN', 0.1],
              ['TID_SCAN', 0.1],
              ['DERIVED_SCAN', 0.05],
              ['OTHERS', 0.03],
            ])
          : pickWeightedValue(random, [
              ['DERIVED_SCAN', 0.34],
              ['FULL_SCAN', 0.24],
              ['INDEX_SCAN', 0.16],
              ['TID_SCAN', 0.12],
              ['BITMAP_SCAN', 0.08],
              ['OTHERS', 0.06],
            ]);
  const joinBucket: RuntimeSample['joinBucket'] =
    random() < 0.1
      ? 'NONE'
      : pickWeightedValue(random, [
          ['NESTED_LOOP', clamp(0.34 - timePosition * 0.12 + (indexUsed ? 0.08 : 0), 0.12, 0.5)],
          ['HASH_JOIN', clamp(0.24 + timePosition * 0.26 + (fullScan ? 0.08 : 0), 0.16, 0.54)],
          ['MERGE_JOIN', clamp(0.14 + (hintUsed ? 0.06 : 0), 0.08, 0.28)],
          ['CARTESIAN_JOIN', dbms === 'oracle' ? clamp(0.04 + timePosition * 0.08, 0.03, 0.14) : 0],
          ['OTHERS', clamp(0.08 + timePosition * 0.16, 0.06, 0.24)],
        ]);
  const filterBucket: RuntimeSample['filterBucket'] =
    random() < 0.12
      ? 'NONE'
      : pickWeightedValue(random, [
          ['ACCESS_FILTER', clamp(0.34 + (indexUsed ? 0.14 : 0), 0.16, 0.54)],
          ['POST_FILTER', clamp(0.26 + timePosition * 0.18, 0.14, 0.42)],
          ['JOIN_FILTER', clamp(0.12 + timePosition * 0.18, 0.08, 0.3)],
          ['OTHERS', clamp(0.18 + (fullScan ? 0.08 : 0), 0.1, 0.28)],
        ]);
  const sortBucket: RuntimeSample['sortBucket'] =
    dbms === 'oracle'
      ? pickWeightedValue(random, [
          ['NONE', rowsScanned > 85000 ? 0.4 : 0.62],
          ['ORDER_SORT', rowsScanned > 85000 ? 0.22 : 0.14],
          ['GROUP_SORT', hintUsed ? 0.12 : 0.16],
          ['UNIQUE_SORT', 0.08],
          ['WINDOW_SORT', rowsScanned > 70000 ? 0.1 : 0.05],
          ['OTHERS', 0.05],
        ])
      : pickWeightedValue(random, [
          ['NONE', rowsScanned > 85000 ? 0.42 : 0.68],
          ['PLAIN_SORT', rowsScanned > 85000 ? 0.34 : 0.2],
          ['INCREMENTAL_SORT', indexUsed ? 0.18 : 0.06],
          ['OTHERS', 0.06],
        ]);
  const aggregateBucket: RuntimeSample['aggregateBucket'] =
    dbms === 'oracle'
      ? pickWeightedValue(random, [
          ['NONE', hintUsed ? 0.38 : 0.44],
          ['PLAIN_AGG', 0.16],
          ['GROUP_AGG', 0.16],
          ['HASH_AGG', fullScan ? 0.14 : 0.1],
          ['WINDOW_AGG', 0.08],
          ['OTHERS', 0.06],
        ])
      : pickWeightedValue(random, [
          ['NONE', hintUsed ? 0.36 : 0.42],
          ['PLAIN_AGG', 0.14],
          ['GROUP_AGG', 0.1],
          ['HASH_AGG', fullScan ? 0.14 : 0.1],
          ['MIXED_AGG', 0.08],
          ['WINDOW_AGG', 0.06],
          ['UNIQUE_AGG', 0.04],
          ['SET_AGG', 0.03],
          ['OTHERS', 0.03],
        ]);

  return {
    scanBucket,
    joinBucket,
    filterBucket,
    sortBucket,
    aggregateBucket,
  };
}

function createMockSolverRuns(options: RuntimeMockOptions): MockSolverRunResult {
  const random = createSeededRandom(options.seed);
  const metricsRandom = createSeededRandom(options.seed + 211);
  const dbms = options.dbms ?? 'postgresql';
  const fastestTimeMs = randomInt(random, options.fastestRangeMs[0], options.fastestRangeMs[1]);
  const slowestTimeCandidate = randomInt(random, options.slowestRangeMs[0], options.slowestRangeMs[1]);
  const slowestTimeMs = Math.max(slowestTimeCandidate, fastestTimeMs + 35);
  const rangeMs = slowestTimeMs - fastestTimeMs;
  const meanBias = clamp((options.meanBias ?? 0.54) + (random() - 0.5) * 0.06, 0.34, 0.72);
  const spreadFactor = clamp((options.spreadFactor ?? 0.18) + (random() - 0.5) * 0.03, 0.12, 0.26);
  const meanMs = Math.round(fastestTimeMs + rangeMs * meanBias);
  const stdDevMs = Math.max(6, Math.round(rangeMs * spreadFactor));
  const myTimeMs =
    options.myTimeMs === undefined ? undefined : clamp(options.myTimeMs, fastestTimeMs + 5, slowestTimeMs - 5);
  const competitorCount = myTimeMs === undefined ? options.count : options.count - 1;
  const interiorCount = Math.max(0, competitorCount - 2);
  const seededRuns = Array.from({ length: interiorCount }, (_, index) => ({
    nickname: createNickname(index, random),
    timeMs: clamp(Math.round(meanMs + nextGaussian(random) * stdDevMs), fastestTimeMs + 1, slowestTimeMs - 1),
  }));

  seededRuns.push({ nickname: createNickname(interiorCount, random), timeMs: fastestTimeMs });
  seededRuns.push({ nickname: createNickname(interiorCount + 1, random), timeMs: slowestTimeMs });

  if (myTimeMs !== undefined) {
    seededRuns.push({ nickname: 'speedql_me', timeMs: myTimeMs });
  }

  const sortedRuns = seededRuns.sort((left, right) => left.timeMs - right.timeMs || left.nickname.localeCompare(right.nickname));
  const normalizedRangeMs = Math.max(1, slowestTimeMs - fastestTimeMs);
  const runs: MockSolverRun[] = sortedRuns.map((run, index) => ({
    ...run,
    ...(() => {
      const timePosition = clamp((run.timeMs - fastestTimeMs) / normalizedRangeMs, 0, 1);
      const fullScanChance = clamp(0.04 + timePosition * 0.58 + (metricsRandom() - 0.5) * 0.14, 0.02, 0.94);
      const fullScan = metricsRandom() < fullScanChance;
      const indexUsageChance = clamp(0.92 - timePosition * 0.56 + (metricsRandom() - 0.5) * 0.16, 0.08, 0.99);
      const indexUsed = !fullScan && metricsRandom() < indexUsageChance;
      const hintUsageChance = clamp(
        0.07 + timePosition * 0.2 + (fullScan ? 0.08 : 0) + (metricsRandom() - 0.5) * 0.08,
        0.03,
        0.48
      );
      const hintUsed = metricsRandom() < hintUsageChance;
      const rowsScanned = Math.max(
        4200,
        Math.round(
          9000 +
            normalizedRangeMs * 130 +
            timePosition ** 1.35 * randomInt(metricsRandom, 38000, 142000) +
            (fullScan ? randomInt(metricsRandom, 28000, 78000) : 0) -
            (indexUsed ? randomInt(metricsRandom, 2000, 12000) : 0)
        )
      );
      const bufferHitRate = clamp(
        96 - timePosition * 22 - (fullScan ? 11 : 0) + (indexUsed ? 5 : 0) + (metricsRandom() - 0.5) * 6,
        54,
        99.6
      );
      const tempSpillChance = clamp(
        0.03 + timePosition * 0.46 + (rowsScanned >= 90000 ? 0.12 : 0) + (metricsRandom() - 0.5) * 0.12,
        0.01,
        0.86
      );
      const tempSpill = metricsRandom() < tempSpillChance;
      const planBuckets = createPlanBuckets({
        dbms,
        fullScan,
        indexUsed,
        hintUsed,
        rowsScanned,
        timePosition,
        random: metricsRandom,
      });

      return {
        submittedAt: offsetDateTime(options.fastestSolvedAt, index * randomInt(random, 7, 21) + randomInt(random, 0, 8)),
        rowsScanned,
        bufferHitRate: Math.round(bufferHitRate * 10) / 10,
        indexUsed,
        fullScan,
        hintUsed,
        ...planBuckets,
        isMine: run.nickname === 'speedql_me',
        tempSpill,
      };
    })(),
  }));

  return {
    runs,
    slowestTimeMs,
    myTimeMs,
  };
}

function buildRuntimeDistribution(options: RuntimeMockOptions): RuntimeDistribution {
  const bucketSizeMs = 5;
  const { runs, slowestTimeMs, myTimeMs } = createMockSolverRuns(options);
  const statsRandom = createSeededRandom(options.seed + 97);
  const fastestBucketStart = floorToStep(runs[0].timeMs, bucketSizeMs);
  const slowestBoundaryMs = ceilToStep(slowestTimeMs, bucketSizeMs);
  const firstBucketStart = fastestBucketStart - bucketSizeMs;
  const lastBucketStart = slowestBoundaryMs;
  const bucketStarts = Array.from(
    { length: Math.floor((lastBucketStart - firstBucketStart) / bucketSizeMs) + 1 },
    (_, index) => firstBucketStart + index * bucketSizeMs
  );
  const bucketCounts = new Map(bucketStarts.map((startMs) => [startMs, 0]));

  runs.forEach((run) => {
    const normalizedTimeMs = run.timeMs === slowestBoundaryMs ? run.timeMs - 1 : run.timeMs;
    const bucketStart =
      firstBucketStart +
      Math.floor((clamp(normalizedTimeMs, firstBucketStart, slowestBoundaryMs - 1) - firstBucketStart) / bucketSizeMs) *
        bucketSizeMs;

    bucketCounts.set(bucketStart, (bucketCounts.get(bucketStart) ?? 0) + 1);
  });

  const medianLeftIndex = Math.floor((runs.length - 1) / 2);
  const medianRightIndex = Math.floor(runs.length / 2);
  const p90Index = Math.min(runs.length - 1, Math.ceil(runs.length * 0.9) - 1);
  const averageTimeMs = runs.reduce((sum, run) => sum + run.timeMs, 0) / runs.length;
  const medianTimeMs = Math.round((runs[medianLeftIndex].timeMs + runs[medianRightIndex].timeMs) / 2);
  const varianceMs = runs.reduce((sum, run) => sum + (run.timeMs - averageTimeMs) ** 2, 0) / runs.length;
  const standardDeviationMs = Math.sqrt(varianceMs);
  const averageRowsScanned = runs.reduce((sum, run) => sum + run.rowsScanned, 0) / runs.length;
  const bufferHitRate = runs.reduce((sum, run) => sum + run.bufferHitRate, 0) / runs.length;
  const indexUsageRate = (runs.filter((run) => run.indexUsed).length / runs.length) * 100;
  const fullScanRate = (runs.filter((run) => run.fullScan).length / runs.length) * 100;
  const hintUsageRate = (runs.filter((run) => run.hintUsed).length / runs.length) * 100;
  const tempSpillRate = (runs.filter((run) => run.tempSpill).length / runs.length) * 100;
  const submissionCount = Math.round(options.count * (1.55 + statsRandom() * 1.15));

  return {
    bucketSizeMs,
    buckets: bucketStarts.map((startMs) => ({
      startMs,
      count: bucketCounts.get(startMs) ?? 0,
    })),
    fastestTimeMs: runs[0].timeMs,
    fastestNickname: runs[0].nickname,
    fastestSolvedAt: runs[0].submittedAt,
    averageTimeMs: Math.round(averageTimeMs * 10) / 10,
    medianTimeMs,
    standardDeviationMs: Math.round(standardDeviationMs * 10) / 10,
    varianceMs: Math.round(varianceMs * 10) / 10,
    myTimeMs,
    submissionCount,
    topPerformers: runs.slice(0, 5).map((run) => ({
      nickname: run.nickname,
      timeMs: run.timeMs,
      submittedAt: run.submittedAt,
    })),
    samples: runs.map((run) => ({
      nickname: run.nickname,
      timeMs: run.timeMs,
      rowsScanned: run.rowsScanned,
      submittedAt: run.submittedAt,
      indexUsed: run.indexUsed,
      fullScan: run.fullScan,
      hintUsed: run.hintUsed,
      scanBucket: run.scanBucket,
      joinBucket: run.joinBucket,
      filterBucket: run.filterBucket,
      sortBucket: run.sortBucket,
      aggregateBucket: run.aggregateBucket,
      isMine: run.isMine,
    })),
    tuningStats: {
      p90TimeMs: runs[p90Index].timeMs,
      indexUsageRate: Math.round(indexUsageRate * 10) / 10,
      fullScanRate: Math.round(fullScanRate * 10) / 10,
      averageRowsScanned: Math.round(averageRowsScanned),
      bufferHitRate: Math.round(bufferHitRate * 10) / 10,
      tempSpillRate: Math.round(tempSpillRate * 10) / 10,
      hintUsageRate: Math.round(hintUsageRate * 10) / 10,
    },
  };
}

function buildRuntimeDistributions(options: RuntimeMockOptions): RuntimeDistributionByDbms {
  const oracleRandom = createSeededRandom(options.seed + 509);
  const oracleFastOffset = randomInt(oracleRandom, 3, 8);
  const oracleSlowOffset = oracleFastOffset + randomInt(oracleRandom, 8, 16);
  const oracleMyOffset = options.myTimeMs === undefined ? undefined : randomInt(oracleRandom, 4, 11);

  return {
    postgresql: buildRuntimeDistribution({ ...options, dbms: 'postgresql' }),
    oracle: buildRuntimeDistribution({
      ...options,
      dbms: 'oracle',
      seed: options.seed + 5000,
      fastestSolvedAt: offsetDateTime(options.fastestSolvedAt, randomInt(oracleRandom, 36, 360)),
      fastestRangeMs: [
        options.fastestRangeMs[0] + oracleFastOffset,
        options.fastestRangeMs[1] + oracleFastOffset,
      ],
      slowestRangeMs: [
        options.slowestRangeMs[0] + oracleSlowOffset,
        options.slowestRangeMs[1] + oracleSlowOffset,
      ],
      myTimeMs: options.myTimeMs === undefined ? undefined : options.myTimeMs + (oracleMyOffset ?? 0),
      meanBias: clamp((options.meanBias ?? 0.54) + 0.04, 0.34, 0.72),
      spreadFactor: clamp((options.spreadFactor ?? 0.18) + 0.02, 0.12, 0.26),
    }),
  };
}

function buildProblemDetail(problem: ProblemTemplate): ProblemDetail {
  const runtimeDistributions = problem.runtimeOptions
    ? buildRuntimeDistributions({
        count: 1000,
        ...problem.runtimeOptions,
      })
    : undefined;

  return {
    id: problem.id,
    domain: 'rdbms',
    number: problem.number,
    title: problem.title,
    preview: problem.preview,
    tags: problem.tags,
    difficulty: problem.difficulty,
    solvedCount: problem.runtimeOptions ? 1000 : 0,
    solvedAt: problem.solvedAt,
    runtimeDistribution: runtimeDistributions?.postgresql,
    runtimeDistributions,
    description: problem.description,
    schemaInfo: problem.schemaInfo,
    inputExample: problem.inputExample,
    outputExample: problem.outputExample,
    starterSql: problem.starterSql,
    dbmsOptions: ['postgresql', 'oracle'],
    disabledDbms: [],
    mockResult: problem.mockResult,
  };
}

const problemTemplates: ProblemTemplate[] = [
  {
    id: 'p-099',
    number: 99,
    title: 'VIP 고객 세그먼트 재구성',
    preview: '주문, 방문, 환불 이력을 함께 분석해 VIP 고객군을 다시 분류하세요.',
    tags: ['CTE', 'CASE', 'LEFT_JOIN', 'WINDOW', 'GROUP_BY', 'COALESCE'],
    difficulty: '고급',
    solvedAt: '2026-03-26 22:08',
    runtimeOptions: {
      seed: 99,
      fastestSolvedAt: '2026-03-15 06:42',
      fastestRangeMs: [11, 16],
      slowestRangeMs: [470, 560],
      myTimeMs: 318,
      meanBias: 0.69,
      spreadFactor: 0.26,
    },
    description:
      '고객의 주문 빈도, 최근 접속일, 환불 이력을 종합해 VIP, 유지, 이탈 위험 세그먼트를 재분류하세요.',
    schemaInfo:
      'customers(customer_id, joined_at, grade)\norders(order_id, customer_id, ordered_at, total_amount)\nrefunds(refund_id, customer_id, refunded_at, refund_amount)\nvisits(visit_id, customer_id, visited_at)',
    inputExample:
      'customers:\n(C01, 2025-01-01, GOLD)\n(C02, 2025-02-11, SILVER)\norders:\n(O1, C01, 2025-11-01, 120000)\n(O2, C01, 2025-11-13, 88000)',
    outputExample: 'customer_id | segment | score\nC01 | VIP | 94',
    starterSql:
      "WITH order_summary AS (\n  SELECT customer_id, COUNT(*) AS order_count, SUM(total_amount) AS total_amount\n  FROM orders\n  GROUP BY customer_id\n)\nSELECT c.customer_id, order_count, total_amount\nFROM customers c\nLEFT JOIN order_summary os ON os.customer_id = c.customer_id;",
    mockResult: mockSuccessResult,
  },
  {
    id: 'p-101',
    number: 101,
    title: '월별 상위 3개 상품 매출 추출',
    preview: '주문과 상품 테이블을 조인해서 월별 매출이 높은 상품을 추려보세요.',
    tags: ['INNER_JOIN', 'DENSE_RANK', 'GROUP_BY', 'SUM', 'DATE_TRUNC', 'CTE'],
    difficulty: '중급',
    solvedAt: '2026-03-24 21:14',
    runtimeOptions: {
      seed: 101,
      fastestSolvedAt: '2026-03-17 07:54',
      fastestRangeMs: [26, 34],
      slowestRangeMs: [131, 144],
      myTimeMs: 83,
      meanBias: 0.52,
      spreadFactor: 0.18,
    },
    description: '월별 총매출 기준으로 상위 3개 상품을 구하고, 동점은 같은 순위로 처리하세요.',
    schemaInfo:
      'orders(order_id, product_id, order_date, quantity, unit_price)\nproducts(product_id, product_name, category)',
    inputExample: 'orders:\n(1, A12, 2025-11-02, 2, 12000)\n(2, B02, 2025-11-03, 5, 8000)',
    outputExample: 'month | product_id | total_sales\n2025-11 | A12 | 812000',
    starterSql:
      "SELECT\n  TO_CHAR(order_date, 'YYYY-MM') AS month,\n  product_id,\n  SUM(quantity * unit_price) AS total_sales\nFROM orders\nGROUP BY TO_CHAR(order_date, 'YYYY-MM'), product_id;",
    mockResult: mockSuccessResult,
  },
  {
    id: 'p-214',
    number: 214,
    title: '지역별 SLA 지연 주문 비율 계산',
    preview: '배송 약속 시간을 넘긴 주문의 비율을 지역 단위로 집계해보세요.',
    tags: ['CASE_WHEN', 'GROUP_BY', 'AVG', 'FILTER', 'INDEX_HINT'],
    difficulty: '고급',
    runtimeOptions: {
      seed: 214,
      fastestSolvedAt: '2026-03-13 19:11',
      fastestRangeMs: [19, 27],
      slowestRangeMs: [142, 156],
      meanBias: 0.58,
      spreadFactor: 0.17,
    },
    description: '지역별 전체 주문 대비 SLA를 넘긴 주문 비율을 계산하고 높은 순서대로 정렬하세요.',
    schemaInfo:
      'shipments(order_id, branch_id, promised_at, delivered_at)\nbranches(branch_id, branch_name)',
    inputExample:
      'shipments:\n(7001, S01, 2025-11-10 16:00, 2025-11-10 18:30)\n(7002, S01, 2025-11-10 18:00, 2025-11-10 17:59)',
    outputExample: 'branch_id | delayed_ratio\nS01 | 0.31',
    starterSql:
      'SELECT\n  s.branch_id,\n  AVG(CASE WHEN s.delivered_at > s.promised_at THEN 1.0 ELSE 0 END) AS delayed_ratio\nFROM shipments s\nGROUP BY s.branch_id;',
    mockResult: mockFailResult,
  },
  {
    id: 'p-305',
    number: 305,
    title: '최근 30일 재구매 고객 찾기',
    preview: '고객별 주문 간격을 분석해 재구매 고객 목록을 찾아보세요.',
    tags: ['WINDOW', 'LAG', 'DATE_DIFF', 'PARTITION_BY', 'COUNT'],
    difficulty: '중급',
    solvedAt: '2026-03-19 08:42',
    runtimeOptions: {
      seed: 305,
      fastestSolvedAt: '2026-03-15 10:26',
      fastestRangeMs: [22, 31],
      slowestRangeMs: [116, 129],
      myTimeMs: 71,
      meanBias: 0.47,
      spreadFactor: 0.16,
    },
    description: '최근 30일 안에 동일 고객이 2회 이상 구매한 경우를 추출하고 주문 간격 평균을 구하세요.',
    schemaInfo: 'customer_orders(order_id, customer_id, order_at, amount)',
    inputExample: 'customer_orders:\n(1, C001, 2025-11-01, 43000)\n(2, C001, 2025-11-13, 62000)',
    outputExample: 'customer_id | repurchase_count | avg_days_gap\nC001 | 3 | 8.5',
    starterSql:
      "SELECT\n  customer_id,\n  COUNT(*) AS repurchase_count\nFROM customer_orders\nWHERE order_at >= CURRENT_DATE - INTERVAL '30 days'\nGROUP BY customer_id;",
    mockResult: mockSuccessResult,
  },
  {
    id: 'p-417',
    number: 417,
    title: '휴면 고객 쿠폰 발급 대상 추출',
    preview: '최근 구매 이력이 없는 고객을 찾아 발급 대상을 추려보세요.',
    tags: ['LEFT_JOIN', 'COALESCE', 'DATE_COMPARE', 'HAVING', 'FILTER'],
    difficulty: '입문',
    description: '90일 이상 구매 이력이 없는 고객을 찾아 쿠폰 발급 대상 목록을 생성하세요.',
    schemaInfo: 'customers(customer_id, status, joined_at)\norders(order_id, customer_id, ordered_at)',
    inputExample: 'customers:\n(C001, active, 2024-01-10)\n(C002, dormant, 2023-07-15)',
    outputExample: 'customer_id | coupon_type\nC002 | WELCOME_BACK',
    starterSql:
      'SELECT\n  c.customer_id\nFROM customers c\nLEFT JOIN orders o ON o.customer_id = c.customer_id\nGROUP BY c.customer_id;',
    mockResult: mockSuccessResult,
  },
  {
    id: 'p-522',
    number: 522,
    title: '결제 수단별 승인 성공률 집계',
    preview: '결제 로그를 기준으로 수단별 승인 성공률을 계산해보세요.',
    tags: ['CASE_WHEN', 'GROUP_BY', 'COUNT', 'CAST', 'ROUND'],
    difficulty: '입문',
    runtimeOptions: {
      seed: 522,
      fastestSolvedAt: '2026-03-12 11:07',
      fastestRangeMs: [31, 42],
      slowestRangeMs: [148, 165],
      meanBias: 0.55,
      spreadFactor: 0.19,
    },
    description: '결제 수단별 승인 성공률을 백분율로 계산하고 성공률이 높은 순으로 정렬하세요.',
    schemaInfo: 'payments(payment_id, method, approved, approved_at)',
    inputExample: 'payments:\n(1, card, true, 2025-11-03 10:20)\n(2, bank, false, 2025-11-03 10:40)',
    outputExample: 'method | approval_rate\ncard | 97.3',
    starterSql:
      'SELECT\n  method,\n  ROUND(AVG(CASE WHEN approved THEN 100.0 ELSE 0 END), 1) AS approval_rate\nFROM payments\nGROUP BY method;',
    mockResult: mockSuccessResult,
  },
  {
    id: 'p-608',
    number: 608,
    title: '주간 이탈 고객 경고 대상 추출',
    preview: '최근 방문과 구매 이력을 함께 보고 이탈 위험 고객을 뽑아보세요.',
    tags: ['LEFT_JOIN', 'MAX', 'DATE_DIFF', 'CASE_WHEN', 'ORDER_BY'],
    difficulty: '중급',
    solvedAt: '2026-03-22 13:20',
    runtimeOptions: {
      seed: 608,
      fastestSolvedAt: '2026-03-10 08:51',
      fastestRangeMs: [24, 33],
      slowestRangeMs: [138, 152],
      myTimeMs: 94,
      meanBias: 0.59,
      spreadFactor: 0.17,
    },
    description: '최근 방문일과 최근 구매일을 비교해 이탈 위험 고객을 선별하고 위험도 순으로 정렬하세요.',
    schemaInfo: 'visits(customer_id, visited_at)\norders(order_id, customer_id, ordered_at)',
    inputExample: 'visits:\n(C001, 2025-11-10)\norders:\n(91, C001, 2025-10-01)',
    outputExample: 'customer_id | risk_level\nC001 | HIGH',
    starterSql:
      'SELECT\n  v.customer_id,\n  MAX(v.visited_at) AS last_visit\nFROM visits v\nGROUP BY v.customer_id;',
    mockResult: mockSuccessResult,
  },
  {
    id: 'p-731',
    number: 731,
    title: '지점별 재고 부족 일수 계산',
    preview: '일별 재고 스냅샷에서 재고 부족 상태가 이어진 기간을 집계하세요.',
    tags: ['LAG', 'SUM', 'CASE_WHEN', 'PARTITION_BY', 'DATE_TRUNC'],
    difficulty: '고급',
    runtimeOptions: {
      seed: 731,
      fastestSolvedAt: '2026-03-11 20:12',
      fastestRangeMs: [29, 38],
      slowestRangeMs: [128, 146],
      meanBias: 0.53,
      spreadFactor: 0.18,
    },
    description: '지점별 재고 부족 상태가 몇 일간 이어졌는지 계산하고 부족 일수가 긴 지점을 먼저 보여주세요.',
    schemaInfo: 'stock_daily(branch_id, snapshot_date, stock_qty)',
    inputExample: 'stock_daily:\n(S01, 2025-11-01, 0)\n(S01, 2025-11-02, 4)',
    outputExample: 'branch_id | shortage_days\nS01 | 5',
    starterSql:
      'SELECT\n  branch_id,\n  SUM(CASE WHEN stock_qty = 0 THEN 1 ELSE 0 END) AS shortage_days\nFROM stock_daily\nGROUP BY branch_id;',
    mockResult: mockFailResult,
  },
  {
    id: 'p-842',
    number: 842,
    title: '프로모션 적용 후 객단가 변화 추적',
    preview: '프로모션 전후 주문 데이터를 비교해 객단가 변화를 계산하세요.',
    tags: ['CTE', 'AVG', 'CASE_WHEN', 'BETWEEN', 'GROUP_BY'],
    difficulty: '중급',
    solvedAt: '2026-03-21 09:18',
    runtimeOptions: {
      seed: 842,
      fastestSolvedAt: '2026-03-09 14:33',
      fastestRangeMs: [18, 26],
      slowestRangeMs: [101, 116],
      myTimeMs: 67,
      meanBias: 0.5,
      spreadFactor: 0.15,
    },
    description: '프로모션 시작 전후 2주 동안의 객단가를 계산해 증가 폭이 큰 캠페인을 찾으세요.',
    schemaInfo: 'orders(order_id, campaign_id, ordered_at, amount)\ncampaigns(campaign_id, started_at)',
    inputExample: 'orders:\n(1, P11, 2025-11-01, 32000)\n(2, P11, 2025-11-14, 47000)',
    outputExample: 'campaign_id | avg_delta\nP11 | 8200',
    starterSql:
      'WITH promo_orders AS (\n  SELECT campaign_id, amount, ordered_at\n  FROM orders\n)\nSELECT campaign_id, AVG(amount) AS avg_amount\nFROM promo_orders\nGROUP BY campaign_id;',
    mockResult: mockSuccessResult,
  },
  {
    id: 'p-905',
    number: 905,
    title: '광고 채널별 전환 퍼널 누락 구간 탐지',
    preview: '채널별 퍼널 단계 누락이 많은 구간을 찾아보세요.',
    tags: ['FILTER', 'COUNT', 'JOIN', 'CASE_WHEN', 'ORDER_BY'],
    difficulty: '고급',
    runtimeOptions: {
      seed: 905,
      fastestSolvedAt: '2026-03-14 16:48',
      fastestRangeMs: [35, 44],
      slowestRangeMs: [160, 176],
      meanBias: 0.57,
      spreadFactor: 0.19,
    },
    description: '광고 채널별 퍼널 단계별 유실률을 계산해 가장 많이 누락되는 구간을 찾아주세요.',
    schemaInfo: 'funnel_steps(session_id, channel, step_name, stepped_at)',
    inputExample: 'funnel_steps:\n(AA1, search, landing, 2025-11-01)\n(AA1, search, cart, 2025-11-01)',
    outputExample: 'channel | weakest_step | drop_rate\nsearch | checkout | 0.42',
    starterSql:
      'SELECT\n  channel,\n  step_name,\n  COUNT(*) AS step_count\nFROM funnel_steps\nGROUP BY channel, step_name;',
    mockResult: mockFailResult,
  },
  {
    id: 'p-1017',
    number: 1017,
    title: '일별 활성 판매자 재방문 비율 분석',
    preview: '활성 판매자 중 다시 방문한 비율을 날짜별로 계산해보세요.',
    tags: ['DATE_TRUNC', 'COUNT_DISTINCT', 'CASE_WHEN', 'WINDOW', 'CTE'],
    difficulty: '중급',
    solvedAt: '2026-03-18 22:06',
    runtimeOptions: {
      seed: 1017,
      fastestSolvedAt: '2026-03-16 07:25',
      fastestRangeMs: [27, 35],
      slowestRangeMs: [134, 149],
      myTimeMs: 88,
      meanBias: 0.56,
      spreadFactor: 0.17,
    },
    description: '일별 활성 판매자 중 7일 내 재방문한 비율을 계산하고 추세를 확인하세요.',
    schemaInfo: 'seller_visits(seller_id, visited_at)',
    inputExample: 'seller_visits:\n(S01, 2025-11-01)\n(S01, 2025-11-05)',
    outputExample: 'visit_date | revisit_ratio\n2025-11-01 | 0.38',
    starterSql:
      "SELECT\n  DATE_TRUNC('day', visited_at) AS visit_date,\n  COUNT(DISTINCT seller_id) AS active_sellers\nFROM seller_visits\nGROUP BY DATE_TRUNC('day', visited_at);",
    mockResult: mockSuccessResult,
  },
  {
    id: 'p-1133',
    number: 1133,
    title: '상품 카테고리별 반품 집중 시간대 찾기',
    preview: '반품이 몰리는 시간대를 카테고리 기준으로 분석하세요.',
    tags: ['EXTRACT', 'GROUP_BY', 'COUNT', 'RANK', 'SUBQUERY'],
    difficulty: '중급',
    runtimeOptions: {
      seed: 1133,
      fastestSolvedAt: '2026-03-08 10:02',
      fastestRangeMs: [21, 29],
      slowestRangeMs: [112, 127],
      meanBias: 0.51,
      spreadFactor: 0.15,
    },
    description: '카테고리별 반품 접수 시간이 가장 많이 몰리는 시간대를 찾아 카테고리와 함께 출력하세요.',
    schemaInfo: 'returns(return_id, category_id, requested_at)',
    inputExample: 'returns:\n(1, C10, 2025-11-01 09:15)\n(2, C10, 2025-11-01 09:43)',
    outputExample: 'category_id | peak_hour\nC10 | 9',
    starterSql:
      'SELECT\n  category_id,\n  EXTRACT(HOUR FROM requested_at) AS requested_hour,\n  COUNT(*) AS return_count\nFROM returns\nGROUP BY category_id, EXTRACT(HOUR FROM requested_at);',
    mockResult: mockSuccessResult,
  },
  {
    id: 'p-1250',
    number: 1250,
    title: '멤버십 등급 승급 후보자 계산',
    preview: '최근 누적 실적을 바탕으로 등급 승급 후보자를 추려보세요.',
    tags: ['SUM', 'HAVING', 'GROUP_BY', 'JOIN', 'CASE_WHEN'],
    difficulty: '입문',
    runtimeOptions: {
      seed: 1250,
      fastestSolvedAt: '2026-03-07 15:14',
      fastestRangeMs: [38, 47],
      slowestRangeMs: [166, 184],
      meanBias: 0.6,
      spreadFactor: 0.18,
    },
    description: '최근 90일 누적 결제 금액과 주문 수를 바탕으로 등급 승급 후보자를 찾으세요.',
    schemaInfo: 'members(member_id, grade)\norders(order_id, member_id, ordered_at, amount)',
    inputExample: 'orders:\n(11, M01, 2025-10-11, 38000)\n(12, M01, 2025-10-22, 55000)',
    outputExample: 'member_id | next_grade\nM01 | gold',
    starterSql:
      "SELECT\n  o.member_id,\n  SUM(o.amount) AS total_amount\nFROM orders o\nWHERE o.ordered_at >= CURRENT_DATE - INTERVAL '90 days'\nGROUP BY o.member_id;",
    mockResult: mockSuccessResult,
  },
  {
    id: 'p-1378',
    number: 1378,
    title: '정산 지연 정산서 우선 처리 대상 추출',
    preview: '정산 예정일을 넘긴 정산서 중 우선 처리 대상을 추리세요.',
    tags: ['CASE_WHEN', 'DATEDIFF', 'ORDER_BY', 'JOIN', 'LIMIT'],
    difficulty: '고급',
    solvedAt: '2026-03-25 18:44',
    runtimeOptions: {
      seed: 1378,
      fastestSolvedAt: '2026-03-18 06:40',
      fastestRangeMs: [33, 41],
      slowestRangeMs: [150, 168],
      myTimeMs: 102,
      meanBias: 0.57,
      spreadFactor: 0.18,
    },
    description: '정산 예정일을 초과한 정산서의 지연 일수를 계산해 우선 처리 대상을 정렬하세요.',
    schemaInfo: 'settlements(settlement_id, seller_id, due_at, settled_at)',
    inputExample: 'settlements:\n(ST1, S01, 2025-11-05, null)\n(ST2, S02, 2025-11-02, 2025-11-04)',
    outputExample: 'settlement_id | overdue_days\nST1 | 6',
    starterSql:
      'SELECT\n  settlement_id,\n  seller_id,\n  due_at,\n  settled_at\nFROM settlements\nWHERE settled_at IS NULL OR settled_at > due_at;',
    mockResult: mockFailResult,
  },
  {
    id: 'p-1492',
    number: 1492,
    title: '신규 제휴사 첫 거래일 계산',
    preview: '제휴사별 첫 거래일을 찾아 온보딩 현황을 정리하세요.',
    tags: ['MIN', 'GROUP_BY', 'JOIN', 'DATE_TRUNC', 'DISTINCT'],
    difficulty: '입문',
    description: '제휴사별 첫 거래일과 첫 거래 월을 추출해 온보딩 현황을 정리하세요.',
    schemaInfo: 'partners(partner_id, joined_at)\ntransactions(tx_id, partner_id, transacted_at)',
    inputExample: 'transactions:\n(T1, P01, 2025-11-01)\n(T2, P01, 2025-11-11)',
    outputExample: 'partner_id | first_traded_at\nP01 | 2025-11-01',
    starterSql:
      'SELECT\n  partner_id,\n  MIN(transacted_at) AS first_traded_at\nFROM transactions\nGROUP BY partner_id;',
    mockResult: mockSuccessResult,
  },
];

export const mockProblemDetails: ProblemDetail[] = problemTemplates.map(buildProblemDetail);

export const mockProblemDetailById = Object.fromEntries(
  mockProblemDetails.map((problem) => [problem.id, problem])
);
