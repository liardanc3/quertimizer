import { useState, type MouseEvent } from 'react';
import { createPortal } from 'react-dom';
import { useEffect } from 'react';
import type {
  AggregateBucket,
  DbmsType,
  FilterBucket,
  JoinBucket,
  ProblemSummary,
  RuntimeSample,
  ScanBucket,
  SortBucket,
} from '../../types/domain';

const TARGET_BUCKET_COUNT = 40;
const numberFormatter = new Intl.NumberFormat('ko-KR');

type MarkerKey = 'fastest' | 'median' | 'mine';
type MarkerTone = 'fastest' | 'median' | 'mine';
type MetricMode = 'time' | 'scanRows';
type FilterMatchMode = 'and' | 'or';
type StatTone = 'neutral' | 'accent' | 'success' | 'warning';
type RuntimeBucketFilterKey =
  | 'scanBucket'
  | 'joinBucket'
  | 'filterBucket'
  | 'sortBucket'
  | 'aggregateBucket';
type BucketFilterValue = ScanBucket | JoinBucket | FilterBucket | SortBucket | AggregateBucket;
type PlanSectionKey = RuntimeBucketFilterKey | 'hint';
type HintFilterValue = 'USED' | 'UNUSED';

interface RuntimeMarker {
  key: MarkerKey;
  label: string;
  value: number;
  tone: MarkerTone;
  nickname?: string;
  submittedAt?: string;
}

interface PlacedMarker extends RuntimeMarker {
  rowIndex: number;
  targetPercent: number;
}

interface StatItemProps {
  label: string;
  value: string;
  detail?: string;
  tone?: StatTone;
}

interface BucketView {
  startValue: number;
  count: number;
}

interface BucketModel {
  bucketSize: number;
  buckets: BucketView[];
  minValue: number;
  minBucketStart: number;
  maxValue: number;
}

interface ProblemRuntimeChartProps {
  problem: ProblemSummary;
  onSearchSelect: (value: string) => void;
}

interface BucketFilterDefinition {
  key: RuntimeBucketFilterKey;
  label: string;
  options: readonly BucketFilterValue[];
}

interface MetricSummary {
  min: number;
  average: number;
  median: number;
  spreadRate: number;
}

interface FloatingTooltipState {
  id: string;
  text: string;
  x: number;
  y: number;
}

const DBMS_OPTIONS: { key: DbmsType; label: string }[] = [
  { key: 'postgresql', label: 'PostgreSQL' },
  { key: 'oracle', label: 'Oracle' },
];

const FILTER_MODE_OPTIONS: { key: FilterMatchMode; label: string }[] = [
  { key: 'and', label: 'AND' },
  { key: 'or', label: 'OR' },
];

const HINT_FILTER_OPTIONS: readonly HintFilterValue[] = ['USED', 'UNUSED'];
const HINT_FILTER_DISPLAY_ORDER: readonly HintFilterValue[] = ['UNUSED', 'USED'];
const ALL_HINT_FILTERS: HintFilterValue[] = [...HINT_FILTER_OPTIONS];

const PLAN_SECTION_OPTIONS: { key: PlanSectionKey; label: string }[] = [
  { key: 'scanBucket', label: 'Scan' },
  { key: 'joinBucket', label: 'Join' },
  { key: 'filterBucket', label: 'Filter' },
  { key: 'sortBucket', label: 'Sort' },
  { key: 'aggregateBucket', label: 'Aggregate' },
  { key: 'hint', label: 'Hint' },
];

const METRIC_OPTIONS: { key: MetricMode; label: string }[] = [
  { key: 'time', label: '실행시간(ms)' },
  { key: 'scanRows', label: 'Scan Rows' },
];

const BUCKET_FILTERS_BY_DBMS: Record<DbmsType, BucketFilterDefinition[]> = {
  postgresql: [
    {
      key: 'scanBucket',
      label: 'Scan',
      options: ['FULL_SCAN', 'INDEX_SCAN', 'BITMAP_SCAN', 'TID_SCAN', 'DERIVED_SCAN', 'OTHERS'],
    },
    {
      key: 'joinBucket',
      label: 'Join',
      options: ['NONE', 'NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'OTHERS'],
    },
    {
      key: 'filterBucket',
      label: 'Filter',
      options: ['NONE', 'ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'OTHERS'],
    },
    { key: 'sortBucket', label: 'Sort', options: ['NONE', 'PLAIN_SORT', 'INCREMENTAL_SORT', 'OTHERS'] },
    {
      key: 'aggregateBucket',
      label: 'Aggregate',
      options: ['NONE', 'PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'MIXED_AGG', 'WINDOW_AGG', 'UNIQUE_AGG', 'SET_AGG', 'OTHERS'],
    },
  ],
  oracle: [
    {
      key: 'scanBucket',
      label: 'Scan',
      options: ['FULL_SCAN', 'ROWID_ACCESS', 'INDEX_SCAN', 'BITMAP_SCAN', 'DERIVED_SCAN', 'REMOTE_SCAN', 'OTHERS'],
    },
    {
      key: 'joinBucket',
      label: 'Join',
      options: ['NONE', 'NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'CARTESIAN_JOIN', 'OTHERS'],
    },
    {
      key: 'filterBucket',
      label: 'Filter',
      options: ['NONE', 'ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'OTHERS'],
    },
    {
      key: 'sortBucket',
      label: 'Sort',
      options: ['NONE', 'ORDER_SORT', 'GROUP_SORT', 'UNIQUE_SORT', 'WINDOW_SORT', 'OTHERS'],
    },
    {
      key: 'aggregateBucket',
      label: 'Aggregate',
      options: ['NONE', 'PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'WINDOW_AGG', 'OTHERS'],
    },
  ],
};

const DEFAULT_BUCKET_FILTERS: Record<RuntimeBucketFilterKey, BucketFilterValue[]> = {
  scanBucket: ['FULL_SCAN', 'ROWID_ACCESS', 'INDEX_SCAN', 'BITMAP_SCAN', 'TID_SCAN', 'DERIVED_SCAN', 'REMOTE_SCAN', 'OTHERS'],
  joinBucket: ['NONE', 'NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'CARTESIAN_JOIN', 'OTHERS'],
  filterBucket: ['NONE', 'ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'OTHERS'],
  sortBucket: ['NONE', 'PLAIN_SORT', 'INCREMENTAL_SORT', 'ORDER_SORT', 'GROUP_SORT', 'UNIQUE_SORT', 'WINDOW_SORT', 'OTHERS'],
  aggregateBucket: ['NONE', 'PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'MIXED_AGG', 'WINDOW_AGG', 'UNIQUE_AGG', 'SET_AGG', 'OTHERS'],
};

function buildAvailableBucketFilters(dbmsKeys: DbmsType[]) {
  const filterMap = new Map<RuntimeBucketFilterKey, { label: string; options: BucketFilterValue[] }>();

  dbmsKeys.forEach((dbmsKey) => {
    BUCKET_FILTERS_BY_DBMS[dbmsKey].forEach((filter) => {
      const existing = filterMap.get(filter.key);

      if (!existing) {
        filterMap.set(filter.key, { label: filter.label, options: [...filter.options] });
        return;
      }

      filter.options.forEach((option) => {
        if (!existing.options.includes(option)) {
          existing.options.push(option);
        }
      });
    });
  });

  return (Object.keys(DEFAULT_BUCKET_FILTERS) as RuntimeBucketFilterKey[]).flatMap((key) => {
    const filter = filterMap.get(key);
    return filter ? [{ key, label: filter.label, options: filter.options }] : [];
  });
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function formatPercent(value: number) {
  return `${numberFormatter.format(Math.round(value * 10) / 10)}%`;
}

function formatCount(value: number) {
  return numberFormatter.format(Math.round(value));
}

function formatMetricAxisLabel(value: number, metricMode: MetricMode) {
  return metricMode === 'time' ? String(Math.round(value)) : formatCount(value);
}

function formatMetricValue(value: number, metricMode: MetricMode) {
  return metricMode === 'time' ? `${Math.round(value * 10) / 10}ms` : `${formatCount(value)} rows`;
}

function getQuantile(sortedValues: number[], quantile: number) {
  if (sortedValues.length === 0) {
    return 0;
  }

  const position = (sortedValues.length - 1) * quantile;
  const lowerIndex = Math.floor(position);
  const upperIndex = Math.ceil(position);
  const interpolation = position - lowerIndex;
  const lowerValue = sortedValues[lowerIndex];
  const upperValue = sortedValues[upperIndex] ?? lowerValue;

  return lowerValue + (upperValue - lowerValue) * interpolation;
}

function formatBucketDisplayLabel(value: BucketFilterValue) {
  if (value === 'NONE') {
    return '없음';
  }

  const normalizedSource = value.toLowerCase().endsWith('_agg') ? value.toLowerCase().replace(/_agg$/, '') : value.toLowerCase();
  const normalized = normalizedSource.replaceAll('_', ' ');
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function getBucketTooltipText(dbms: DbmsType, filterKey: RuntimeBucketFilterKey, value: BucketFilterValue) {
  if (dbms === 'oracle') {
    switch (filterKey) {
      case 'scanBucket':
        switch (value) {
          case 'FULL_SCAN':
            return 'TABLE ACCESS FULL, TABLE ACCESS STORAGE FULL, FIXED TABLE FULL 같은 전체 테이블 읽기를 묶습니다.';
          case 'ROWID_ACCESS':
            return 'TABLE ACCESS BY INDEX ROWID, TABLE ACCESS BY INDEX ROWID BATCHED처럼 ROWID를 따라 테이블 블록을 읽는 접근을 묶습니다.';
          case 'INDEX_SCAN':
            return 'INDEX UNIQUE SCAN, INDEX RANGE SCAN, INDEX FULL SCAN, INDEX FAST FULL SCAN, INDEX SKIP SCAN, INDEX JOIN SCAN 같은 인덱스 접근을 묶습니다.';
          case 'BITMAP_SCAN':
            return 'BITMAP INDEX 계열, BITMAP CONVERSION, BITMAP MERGE, BITMAP AND/OR 같은 비트맵 기반 접근을 묶습니다.';
          case 'DERIVED_SCAN':
            return 'VIEW, MATERIALIZE, WITH, COLLECTION ITERATOR, TEMP TABLE TRANSFORMATION 같은 파생 결과 접근을 묶습니다.';
          case 'REMOTE_SCAN':
            return 'REMOTE 연산처럼 원격 객체를 읽는 접근을 묶습니다.';
          case 'OTHERS':
            return '위 분류로 묶지 않은 기타 스캔·접근 연산입니다.';
          case 'NONE':
            return '해당 실행 계획에서 스캔 분류를 따로 잡지 않은 상태입니다.';
        }
        break;
      case 'joinBucket':
        switch (value) {
          case 'NESTED_LOOP':
            return 'NESTED LOOPS 계열입니다. 바깥 결과를 기준으로 안쪽 경로를 반복 탐색하는 조인입니다.';
          case 'MERGE_JOIN':
            return 'MERGE JOIN 계열입니다. 정렬된 두 입력을 병합하면서 조인하는 방식입니다.';
          case 'HASH_JOIN':
            return 'HASH JOIN 계열입니다. 한쪽 입력으로 해시 테이블을 만들고 다른 쪽을 probe 하는 방식입니다.';
          case 'CARTESIAN_JOIN':
            return 'MERGE JOIN CARTESIAN 같은 카테시안 조인을 묶습니다.';
          case 'OTHERS':
            return '위 네 가지로 분류하지 않은 기타 조인 형태입니다.';
          case 'NONE':
            return '조인 노드가 없거나 조인을 별도 분류하지 않은 경우입니다.';
        }
        break;
      case 'filterBucket':
        switch (value) {
          case 'ACCESS_FILTER':
            return 'Predicate Information의 access(...)처럼 접근 단계에서 읽을 범위를 줄이는 조건을 묶습니다.';
          case 'POST_FILTER':
            return 'Predicate Information의 filter(...)처럼 읽은 뒤 추가로 행을 거르는 조건을 묶습니다.';
          case 'JOIN_FILTER':
            return 'JOIN FILTER CREATE, JOIN FILTER USE 같은 조인 필터 계열을 묶습니다.';
          case 'OTHERS':
            return '위 분류로 묶지 않은 기타 predicate 조건입니다.';
          case 'NONE':
            return '별도 predicate 조건이 보이지 않는 경우입니다.';
        }
        break;
      case 'sortBucket':
        switch (value) {
          case 'ORDER_SORT':
            return 'SORT ORDER BY, SORT ORDER BY STOPKEY 같은 정렬을 묶습니다.';
          case 'GROUP_SORT':
            return 'SORT GROUP BY 같은 그룹 정렬을 묶습니다.';
          case 'UNIQUE_SORT':
            return 'SORT UNIQUE 같은 중복 제거 정렬을 묶습니다.';
          case 'WINDOW_SORT':
            return 'WINDOW SORT 같은 윈도 함수용 정렬을 묶습니다.';
          case 'OTHERS':
            return '위 정렬 유형으로 분류하지 않은 기타 정렬 관련 연산입니다.';
          case 'NONE':
            return '정렬 연산이 없는 경우입니다.';
          case 'PLAIN_SORT':
          case 'INCREMENTAL_SORT':
            return 'Oracle 쪽에서는 사용하지 않는 정렬 분류입니다.';
        }
        break;
      case 'aggregateBucket':
        switch (value) {
          case 'PLAIN_AGG':
            return 'SORT AGGREGATE 같은 단순 집계를 묶습니다.';
          case 'GROUP_AGG':
            return 'SORT GROUP BY, GROUP BY NOSORT 같은 그룹 집계를 묶습니다.';
          case 'HASH_AGG':
            return 'HASH GROUP BY 같은 해시 기반 집계를 묶습니다.';
          case 'WINDOW_AGG':
            return '윈도 함수 계산과 연결된 집계 계열을 묶습니다.';
          case 'OTHERS':
            return 'HASH UNIQUE, SORT UNIQUE, UNION, INTERSECT, MINUS 같은 기타 집계·집합 처리 연산을 묶습니다.';
          case 'NONE':
            return '집계 관련 연산이 없는 경우입니다.';
          case 'MIXED_AGG':
          case 'UNIQUE_AGG':
          case 'SET_AGG':
            return 'Oracle 쪽에서는 기타 집계 계열로 함께 묶는 분류입니다.';
        }
        break;
    }
  }

  switch (filterKey) {
    case 'scanBucket':
      switch (value) {
        case 'FULL_SCAN':
          return 'Seq Scan 계열입니다. 인덱스를 타지 않고 테이블 페이지를 순차적으로 읽는 경우를 묶습니다.';
        case 'INDEX_SCAN':
          return 'Index Scan, Index Only Scan 계열입니다. 인덱스를 직접 따라가며 필요한 튜플만 찾는 경우를 묶습니다.';
        case 'BITMAP_SCAN':
          return 'Bitmap Index Scan + Bitmap Heap Scan 계열입니다. 인덱스 결과를 비트맵으로 모은 뒤 힙을 다시 읽는 경우를 뜻합니다.';
        case 'TID_SCAN':
          return 'Tid Scan, Tid Range Scan 계열입니다. 튜플 식별자(TID)를 직접 사용해 특정 위치를 읽는 경우를 묶습니다.';
        case 'DERIVED_SCAN':
          return 'Subquery Scan, Function Scan, Table Function Scan, Values Scan, CTE Scan, WorkTable Scan 같은 파생 스캔을 묶습니다.';
        case 'OTHERS':
          return '위 분류로 명확히 묶기 어려운 기타 스캔 노드입니다.';
        case 'NONE':
          return '해당 실행 계획에서 스캔 분류를 따로 잡지 않은 상태입니다.';
        case 'ROWID_ACCESS':
        case 'REMOTE_SCAN':
          return 'PostgreSQL 쪽에서는 사용하지 않는 스캔 분류입니다.';
      }
      break;
    case 'joinBucket':
      switch (value) {
        case 'NESTED_LOOP':
          return 'Nested Loop 조인입니다. 바깥 결과를 하나씩 가져오며 안쪽 경로를 반복 탐색합니다.';
        case 'MERGE_JOIN':
          return 'Merge Join입니다. 정렬된 두 입력을 병합하면서 조인하는 방식입니다.';
        case 'HASH_JOIN':
          return 'Hash Join입니다. 한쪽 입력으로 해시 테이블을 만들고 다른 쪽을 probe 하는 방식입니다.';
        case 'OTHERS':
          return '위 세 가지로 분류하지 않은 기타 조인 형태입니다.';
        case 'NONE':
          return '조인 노드가 없거나 조인을 별도 분류하지 않은 경우입니다.';
        case 'CARTESIAN_JOIN':
          return 'PostgreSQL 쪽에서는 사용하지 않는 카테시안 조인 분류입니다.';
      }
      break;
    case 'filterBucket':
      switch (value) {
        case 'ACCESS_FILTER':
          return 'Index Cond, Recheck Cond처럼 접근 단계에서 읽을 범위를 줄이는 조건을 묶습니다.';
        case 'POST_FILTER':
          return 'Filter처럼 읽은 뒤 추가로 행을 걸러내는 조건을 뜻합니다.';
        case 'JOIN_FILTER':
          return 'Join Filter처럼 조인 과정에서 추가로 적용되는 조건을 뜻합니다.';
        case 'OTHERS':
          return '위 조건 라벨로 분류하지 않은 기타 필터 조건입니다.';
        case 'NONE':
          return '별도 필터 조건이 보이지 않는 경우입니다.';
      }
      break;
    case 'sortBucket':
      switch (value) {
        case 'PLAIN_SORT':
          return '일반 Sort 노드입니다. quicksort, external merge, top-N heapsort 같은 일반 정렬을 묶습니다.';
        case 'INCREMENTAL_SORT':
          return 'Incremental Sort 노드입니다. 이미 정렬된 앞부분을 활용해 추가 정렬 비용을 줄이는 방식입니다.';
        case 'OTHERS':
          return '위 정렬 유형으로 분류하지 않은 기타 정렬 관련 노드입니다.';
        case 'NONE':
          return '정렬 노드가 없는 경우입니다.';
        case 'ORDER_SORT':
        case 'GROUP_SORT':
        case 'UNIQUE_SORT':
        case 'WINDOW_SORT':
          return 'PostgreSQL 쪽에서는 사용하지 않는 정렬 분류입니다.';
      }
      break;
    case 'aggregateBucket':
      switch (value) {
        case 'PLAIN_AGG':
          return 'Aggregate 노드입니다. 그룹 키 없이 바로 집계하는 단순 집계를 뜻합니다.';
        case 'GROUP_AGG':
          return 'GroupAggregate 계열입니다. 정렬된 입력을 기준으로 그룹별 집계를 수행합니다.';
        case 'HASH_AGG':
          return 'HashAggregate 계열입니다. 해시 테이블을 사용해 그룹 집계를 수행합니다.';
        case 'MIXED_AGG':
          return 'MixedAggregate 계열입니다. 해시와 정렬 전략이 섞인 집계를 뜻합니다.';
        case 'WINDOW_AGG':
          return 'WindowAgg 노드입니다. 윈도 함수 계산을 수행하는 경우입니다.';
        case 'UNIQUE_AGG':
          return 'Unique 계열입니다. 중복 제거를 통해 집계 성격의 결과를 만드는 경우를 묶습니다.';
        case 'SET_AGG':
          return 'SetOp 계열입니다. UNION, INTERSECT, EXCEPT 같은 집합 연산 기반 처리를 묶습니다.';
        case 'OTHERS':
          return '위 집계 분류로 나누기 어려운 기타 집계 관련 노드입니다.';
        case 'NONE':
          return '집계 관련 노드가 없는 경우입니다.';
      }
      break;
  }

  return '실행 계획 설명입니다.';
}

function formatBucketRange(startValue: number, bucketSize: number, metricMode: MetricMode) {
  const endValue = startValue + bucketSize - 1;
  const startLabel = formatMetricAxisLabel(startValue, metricMode);
  const endLabel = formatMetricAxisLabel(endValue, metricMode);

  return metricMode === 'time' ? `${startLabel}-${endLabel}ms` : `${startLabel}-${endLabel} rows`;
}

function getMetricValue(sample: RuntimeSample, metricMode: MetricMode) {
  return metricMode === 'time' ? sample.timeMs : sample.rowsScanned;
}

function compareMarkers(left: RuntimeMarker, right: RuntimeMarker) {
  if (left.value !== right.value) {
    return left.value - right.value;
  }

  const priority: Record<MarkerKey, number> = {
    fastest: 0,
    median: 1,
    mine: 2,
  };

  return priority[left.key] - priority[right.key];
}

function getVisibleSelectedValues<T extends string>(selectedValues: T[], allOptions: readonly T[]) {
  return allOptions.filter((option) => selectedValues.includes(option));
}

function areAllOptionsSelected<T extends string>(selectedValues: T[], allOptions: readonly T[]) {
  return allOptions.every((option) => selectedValues.includes(option));
}

function buildBucketModel(samples: RuntimeSample[], metricMode: MetricMode): BucketModel | null {
  if (samples.length === 0) {
    return null;
  }

  const values = samples.map((sample) => getMetricValue(sample, metricMode));
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const range = Math.max(1, maxValue - minValue + 1);
  const bucketSize = Math.max(1, Math.ceil(range / TARGET_BUCKET_COUNT));
  const bucketCount = Math.max(1, Math.ceil(range / bucketSize));
  const minBucketStart = minValue;
  const bucketStarts = Array.from({ length: bucketCount }, (_, index) => minBucketStart + index * bucketSize);
  const lastBoundaryValue = minBucketStart + bucketSize * bucketCount;
  const bucketCounts = new Map(bucketStarts.map((startValue) => [startValue, 0]));

  samples.forEach((sample) => {
    const sampleValue = getMetricValue(sample, metricMode);
    const normalizedValue = sampleValue === lastBoundaryValue ? sampleValue - 1 : sampleValue;
    const bucketStart =
      minBucketStart +
      Math.floor((clamp(normalizedValue, minBucketStart, lastBoundaryValue - 1) - minBucketStart) / bucketSize) *
        bucketSize;

    bucketCounts.set(bucketStart, (bucketCounts.get(bucketStart) ?? 0) + 1);
  });

  return {
    bucketSize,
    buckets: bucketStarts.map((startValue) => ({
      startValue,
      count: bucketCounts.get(startValue) ?? 0,
    })),
    minValue,
    minBucketStart,
    maxValue,
  };
}

function buildMarkers(samples: RuntimeSample[], metricMode: MetricMode): RuntimeMarker[] {
  if (samples.length === 0) {
    return [];
  }

  const sortedSamples = [...samples].sort(
    (left, right) =>
      getMetricValue(left, metricMode) - getMetricValue(right, metricMode) || left.nickname.localeCompare(right.nickname)
  );
  const mineSample = sortedSamples.find((sample) => sample.isMine);
  const markers: RuntimeMarker[] = [
    {
      key: 'fastest',
      label: '1st',
      value: getMetricValue(sortedSamples[0], metricMode),
      tone: 'fastest',
      nickname: sortedSamples[0].nickname,
      submittedAt: sortedSamples[0].submittedAt,
    },
  ];

  if (mineSample) {
    markers.push({
      key: 'mine',
      label: '내 기록',
      value: getMetricValue(mineSample, metricMode),
      tone: 'mine',
      submittedAt: mineSample.submittedAt,
    });
  }

  return markers.sort(compareMarkers);
}

function buildMetricSummary(samples: RuntimeSample[], metricMode: MetricMode): MetricSummary | null {
  if (samples.length === 0) {
    return null;
  }

  const values = [...samples.map((sample) => getMetricValue(sample, metricMode))].sort((left, right) => left - right);
  const average = values.reduce((sum, value) => sum + value, 0) / values.length;
  const medianLeftIndex = Math.floor((values.length - 1) / 2);
  const medianRightIndex = Math.floor(values.length / 2);
  const median = (values[medianLeftIndex] + values[medianRightIndex]) / 2;
  const p90 = getQuantile(values, 0.9);
  const min = values[0];
  const denominator = Math.max(Math.abs(median), 1);

  return {
    min,
    average,
    median,
    spreadRate: ((p90 - min) / denominator) * 100,
  };
}

function calculatePercent(samples: RuntimeSample[], predicate: (sample: RuntimeSample) => boolean) {
  if (samples.length === 0) {
    return null;
  }

  return (samples.filter(predicate).length / samples.length) * 100;
}

function normalizeSelectedValues<T extends string>(selectedValues: T[], allOptions: readonly T[]) {
  const uniqueValues = allOptions.filter((option) => selectedValues.includes(option));
  return uniqueValues;
}

function sortHintFilters(values: HintFilterValue[]) {
  return [...values].sort(
    (left, right) => HINT_FILTER_DISPLAY_ORDER.indexOf(left) - HINT_FILTER_DISPLAY_ORDER.indexOf(right)
  );
}

function getDistributionMap(problem: ProblemSummary) {
  if (problem.runtimeDistributions) {
    return problem.runtimeDistributions;
  }

  return problem.runtimeDistribution ? { postgresql: problem.runtimeDistribution } : {};
}

function matchesBucketFilter(sample: RuntimeSample, key: RuntimeBucketFilterKey, value: BucketFilterValue) {
  return sample[key] === value;
}

function StatItem({ label, value, detail, tone = 'neutral' }: StatItemProps) {
  return (
    <div className={`runtime-stat-item is-${tone}`}>
      <span className="runtime-stat-copy">
        <span className="runtime-stat-meta">
          <span className="runtime-stat-label">{label}</span>
          {detail ? <span className="runtime-stat-detail">{detail}</span> : null}
        </span>
      </span>
      <span className="runtime-stat-value">{value}</span>
    </div>
  );
}

function getGroupedSelectionItems(args: {
  availableBucketFilters: BucketFilterDefinition[];
  selectedBucketFilters: Record<RuntimeBucketFilterKey, BucketFilterValue[]>;
  selectedHintFilters: HintFilterValue[];
  activeSamples: RuntimeSample[];
  filterMatchMode: FilterMatchMode;
  selectedPlanSections: PlanSectionKey[];
}) {
  const {
    availableBucketFilters,
    selectedBucketFilters,
    selectedHintFilters,
    activeSamples,
    filterMatchMode,
    selectedPlanSections,
  } = args;
  const modeLabel = filterMatchMode.toUpperCase();

  const bucketItems = availableBucketFilters.flatMap((filter) => {
    if (!selectedPlanSections.includes(filter.key)) {
      return [];
    }

    const selectedValues = getVisibleSelectedValues(selectedBucketFilters[filter.key], filter.options);
    if (selectedValues.length === 0) {
      return [];
    }

    const isAllSelected = areAllOptionsSelected(selectedValues, filter.options);

    const ratio = isAllSelected
      ? 100
      : calculatePercent(activeSamples, (sample) => selectedValues.some((value) => sample[filter.key] === value));

    return [
      {
        id: filter.key,
        label: `${filter.label} 발생 비율 (${modeLabel})`,
        detail: isAllSelected ? '전체' : selectedValues.map((value) => formatBucketDisplayLabel(value)).join(', '),
        value: ratio === null ? '-' : formatPercent(ratio),
        tone: 'neutral' as const,
      },
    ];
  });

  const hintItem =
    !selectedPlanSections.includes('hint') || selectedHintFilters.length === 0
      ? []
      : [
          {
            id: 'hint',
            label: `Hint 사용 비율 (${modeLabel})`,
            detail:
              areAllOptionsSelected(selectedHintFilters, HINT_FILTER_OPTIONS)
                ? '전체'
                : sortHintFilters(selectedHintFilters).map((value) => (value === 'USED' ? '사용' : '미사용')).join(', '),
            value:
              areAllOptionsSelected(selectedHintFilters, HINT_FILTER_OPTIONS)
                ? '100%'
                : formatPercent(
                    calculatePercent(activeSamples, (sample) =>
                      selectedHintFilters.some((value) => (value === 'USED' ? sample.hintUsed : !sample.hintUsed))
                    ) ?? 0
                  ),
            tone: 'neutral' as const,
          },
        ];

  return [...bucketItems, ...hintItem];
}

function renderMarkerTooltip(marker: RuntimeMarker, metricMode: MetricMode, onSearchSelect: (value: string) => void) {
  if (marker.key === 'fastest') {
    return (
      <>
        <span className="ui-tooltip-title">{formatMetricValue(marker.value, metricMode)}</span>
        <span className="ui-tooltip-stack">
          {marker.nickname ? (
            <button
              type="button"
              className="tooltip-link tooltip-link-inline"
              onClick={(event: MouseEvent<HTMLButtonElement>) => {
                event.preventDefault();
                event.stopPropagation();
                onSearchSelect(marker.nickname ?? '');
              }}
            >
              {marker.nickname}
            </button>
          ) : null}
          {marker.submittedAt ? <span className="ui-tooltip-caption">{marker.submittedAt}</span> : null}
        </span>
      </>
    );
  }

  if (marker.key === 'median') {
    return <span className="ui-tooltip-title">{formatMetricValue(marker.value, metricMode)}</span>;
  }

  return (
    <>
      <span className="ui-tooltip-title">{formatMetricValue(marker.value, metricMode)}</span>
      {marker.submittedAt ? <span className="ui-tooltip-caption">{marker.submittedAt}</span> : null}
    </>
  );
}

export default function ProblemRuntimeChart({ problem, onSearchSelect }: ProblemRuntimeChartProps) {
  const distributionMap = getDistributionMap(problem);
  const availableDbms = DBMS_OPTIONS.filter((option) => distributionMap[option.key]);
  const defaultDbmsKeys: DbmsType[] = availableDbms[0] ? [availableDbms[0].key] : ['postgresql'];
  const defaultPlanSections = PLAN_SECTION_OPTIONS.map((section) => section.key);
  const [metricMode, setMetricMode] = useState<MetricMode>('time');
  const [filterMatchMode, setFilterMatchMode] = useState<FilterMatchMode>('or');
  const [selectedDbmsKeys, setSelectedDbmsKeys] = useState<DbmsType[]>(defaultDbmsKeys);
  const [selectedPlanSections, setSelectedPlanSections] = useState<PlanSectionKey[]>(defaultPlanSections);
  const [showPlanDetails, setShowPlanDetails] = useState(false);
  const [selectedBucketFilters, setSelectedBucketFilters] =
    useState<Record<RuntimeBucketFilterKey, BucketFilterValue[]>>(DEFAULT_BUCKET_FILTERS);
  const [selectedHintFilters, setSelectedHintFilters] = useState<HintFilterValue[]>(ALL_HINT_FILTERS);
  const [floatingTooltip, setFloatingTooltip] = useState<FloatingTooltipState | null>(null);
  const validSelectedDbmsKeys = selectedDbmsKeys.filter((dbmsKey) => distributionMap[dbmsKey]);
  const effectiveSelectedDbmsKeys =
    validSelectedDbmsKeys.length > 0 ? validSelectedDbmsKeys : availableDbms[0] ? [availableDbms[0].key] : [];
  const activeDbmsKeys = effectiveSelectedDbmsKeys.slice(0, 1);
  const activeDbmsKey = activeDbmsKeys[0] ?? 'postgresql';
  const activeDistributions = activeDbmsKeys.flatMap((dbmsKey) => {
    const distribution = distributionMap[dbmsKey];
    return distribution ? [distribution] : [];
  });
  const activeSamples = activeDistributions.flatMap((distribution) => distribution.samples);
  const availableBucketFilters = buildAvailableBucketFilters(activeDbmsKeys);
  const normalizedSelectedPlanSections = defaultPlanSections.filter((sectionKey) => selectedPlanSections.includes(sectionKey));
  const allPlanSectionsSelected = normalizedSelectedPlanSections.length === defaultPlanSections.length;
  const bucketFilterSelections = availableBucketFilters.map((filter) => {
    const selectedValues = getVisibleSelectedValues(selectedBucketFilters[filter.key], filter.options);
    return {
      filter,
      selectedValues,
      isAllSelected: areAllOptionsSelected(selectedValues, filter.options),
    };
  });
  const allHintFiltersSelected = areAllOptionsSelected(selectedHintFilters, HINT_FILTER_OPTIONS);
  const hasActiveHintSelection =
    selectedPlanSections.includes('hint') && selectedHintFilters.length > 0 && !allHintFiltersSelected;

  const selectedBucketEntries = bucketFilterSelections.flatMap(({ filter, selectedValues, isAllSelected }) => {
    if (!selectedPlanSections.includes(filter.key) || isAllSelected || selectedValues.length === 0) {
      return [];
    }

    return [[filter.key, selectedValues] as const];
  });

  const filteredSamples =
    selectedBucketEntries.length === 0 &&
        !hasActiveHintSelection
      ? activeSamples
      : activeSamples.filter((sample) => {
          const matches = [
            ...selectedBucketEntries.map(([filterKey, filterValues]) =>
              filterValues.some((filterValue) => matchesBucketFilter(sample, filterKey, filterValue))
            ),
            ...(hasActiveHintSelection
              ? [selectedHintFilters.some((filterValue) => (filterValue === 'USED' ? sample.hintUsed : !sample.hintUsed))]
              : []),
          ];

          return matches.length === 0 ? true : filterMatchMode === 'and' ? matches.every(Boolean) : matches.some(Boolean);
        });

  const timeSummary = buildMetricSummary(filteredSamples, 'time');
  const scanRowsSummary = buildMetricSummary(filteredSamples, 'scanRows');
  const markers = buildMarkers(filteredSamples, metricMode);
  const bucketModel = buildBucketModel(filteredSamples, metricMode);
  const maxBucketCount = bucketModel ? Math.max(1, ...bucketModel.buckets.map((bucket) => bucket.count)) : 1;
  const guideRowCount = 2;
  const placedMarkers: PlacedMarker[] =
    bucketModel === null
      ? []
      : markers.map((marker) => {
          const bucketIndex = clamp(
            Math.floor((marker.value - bucketModel.minBucketStart) / bucketModel.bucketSize),
            0,
            bucketModel.buckets.length - 1
          );

          return {
            ...marker,
            rowIndex: marker.key === 'fastest' ? 1 : 0,
            targetPercent: ((bucketIndex + 0.5) / bucketModel.buckets.length) * 100,
          };
        });
  const markersByRow = [
    placedMarkers.find((marker) => marker.key === 'mine') ?? null,
    placedMarkers.find((marker) => marker.key === 'fastest') ?? null,
  ];
  const firstAxisLabel = bucketModel ? formatMetricAxisLabel(bucketModel.minValue, metricMode) : '';
  const lastAxisLabel = bucketModel ? formatMetricAxisLabel(bucketModel.maxValue, metricMode) : '';
  const selectedRatioItems = getGroupedSelectionItems({
    availableBucketFilters,
    selectedBucketFilters,
    selectedHintFilters,
    activeSamples,
    filterMatchMode,
    selectedPlanSections,
  });

  const toggleFloatingTooltip = (id: string, target: HTMLElement, text: string) => {
    if (floatingTooltip?.id === id) {
      setFloatingTooltip(null);
      return;
    }

    const rect = target.getBoundingClientRect();

    setFloatingTooltip({
      id,
      text,
      x: rect.left + rect.width / 2,
      y: rect.top - 10,
    });
  };

  const hideFloatingTooltip = () => {
    setFloatingTooltip(null);
  };

  useEffect(() => {
    if (!floatingTooltip) {
      return;
    }

    const handlePointerDown = (event: globalThis.MouseEvent) => {
      const target = event.target;

      if (target instanceof HTMLElement && target.closest('.runtime-subfilter-info-button')) {
        return;
      }

      setFloatingTooltip(null);
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setFloatingTooltip(null);
      }
    };

    window.addEventListener('mousedown', handlePointerDown);
    window.addEventListener('resize', hideFloatingTooltip);
    window.addEventListener('scroll', hideFloatingTooltip, true);
    window.addEventListener('keydown', handleEscape);

    return () => {
      window.removeEventListener('mousedown', handlePointerDown);
      window.removeEventListener('resize', hideFloatingTooltip);
      window.removeEventListener('scroll', hideFloatingTooltip, true);
      window.removeEventListener('keydown', handleEscape);
    };
  }, [floatingTooltip]);

  if (activeSamples.length === 0 || problem.solvedCount === 0) {
    return <div className="runtime-empty-state">이 문제의 첫 도전자가 되어보세요</div>;
  }

  return (
    <div className="problem-runtime-shell">
      <div className="problem-runtime-main">
        <div className="runtime-toolbar">
          <div className="runtime-toolbar-primary">
            <div className="runtime-filter-cluster" role="group" aria-label="DBMS 선택">
              <div className="runtime-toolbar-group">
                {availableDbms.map((option) => {
                  const isSelected = activeDbmsKeys.includes(option.key);

                  return (
                    <button
                      key={option.key}
                      type="button"
                      className={`runtime-filter-button ${isSelected ? 'is-selected' : ''}`}
                      aria-pressed={isSelected}
                      onClick={(event) => {
                        event.preventDefault();
                        event.stopPropagation();
                        setSelectedDbmsKeys([option.key]);
                      }}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="runtime-filter-cluster" role="group" aria-label="필터 조합 방식">
              <div className="runtime-toolbar-group">
                {FILTER_MODE_OPTIONS.map((option) => (
                  <button
                    key={option.key}
                    type="button"
                    className={`runtime-filter-button ${filterMatchMode === option.key ? 'is-selected' : ''}`}
                    aria-pressed={filterMatchMode === option.key}
                    onClick={(event) => {
                      event.preventDefault();
                      event.stopPropagation();
                      setFilterMatchMode(option.key);
                    }}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="runtime-filter-cluster is-wide" role="group" aria-label="실행 계획 요소">
              <span className="runtime-filter-cluster-label">실행 계획 요소</span>
              <div className="runtime-filter-group is-plan">
                <button
                  type="button"
                  className={`runtime-filter-button ${allPlanSectionsSelected ? 'is-selected' : ''}`}
                  aria-pressed={allPlanSectionsSelected}
                  onClick={(event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    setSelectedPlanSections((current) =>
                      defaultPlanSections.every((sectionKey) => current.includes(sectionKey)) ? [] : defaultPlanSections
                    );
                  }}
                >
                  전체
                </button>

                {PLAN_SECTION_OPTIONS.map((section) => {
                  const isSelected = normalizedSelectedPlanSections.includes(section.key);

                  return (
                    <button
                      key={section.key}
                      type="button"
                      className={`runtime-filter-button ${isSelected ? 'is-selected' : ''}`}
                      aria-pressed={isSelected}
                      onClick={(event) => {
                        event.preventDefault();
                        event.stopPropagation();
                        setSelectedPlanSections((current) => {
                          const nextValues = current.includes(section.key)
                            ? current.filter((currentKey) => currentKey !== section.key)
                            : [...current, section.key];

                          return defaultPlanSections.filter((sectionKey) => nextValues.includes(sectionKey));
                        }
                        );
                      }}
                    >
                      {section.label}
                    </button>
                  );
                })}
              </div>
              <button
                type="button"
                className="runtime-detail-toggle"
                aria-expanded={showPlanDetails}
                aria-label={showPlanDetails ? '실행 계획 요소 상세 접기' : '실행 계획 요소 상세 펼치기'}
                onClick={(event) => {
                  event.preventDefault();
                  event.stopPropagation();
                  setShowPlanDetails((value) => !value);
                }}
              >
                {showPlanDetails ? '▴' : '▾'}
              </button>
            </div>
          </div>
        </div>

        {normalizedSelectedPlanSections.length > 0 && showPlanDetails ? (
          <div className="runtime-subfilter-board" role="group" aria-label="실행 계획 요소 세부 선택">
            {availableBucketFilters
              .filter((filter) => normalizedSelectedPlanSections.includes(filter.key))
              .map((filter) => {
                const selectedValues = selectedBucketFilters[filter.key];
                const visibleSelectedValues = getVisibleSelectedValues(selectedValues, filter.options);
                const isAllSelected = areAllOptionsSelected(visibleSelectedValues, filter.options);

                return (
                  <div key={filter.key} className="runtime-subfilter-row">
                    <span className="runtime-subfilter-label">{filter.label}</span>
                    <div className="runtime-subfilter-options">
                      <button
                        type="button"
                        className={`runtime-subfilter-button ${isAllSelected ? 'is-selected' : ''}`}
                        aria-pressed={isAllSelected}
                        onClick={(event) => {
                          event.preventDefault();
                          event.stopPropagation();
                          setSelectedBucketFilters((current) => ({
                            ...current,
                            [filter.key]: isAllSelected ? [] : [...filter.options],
                          }));
                        }}
                      >
                        전체
                      </button>

                      {filter.options.map((option) => {
                        const tooltipId = `${filter.key}-${option}`;

                        return (
                          <span
                            key={option}
                            className={`runtime-subfilter-option ${visibleSelectedValues.includes(option) ? 'is-selected' : ''}`}
                          >
                            <button
                              type="button"
                              className="runtime-subfilter-button runtime-subfilter-button-plain"
                              aria-pressed={visibleSelectedValues.includes(option)}
                              onClick={(event) => {
                                event.preventDefault();
                                event.stopPropagation();
                                setSelectedBucketFilters((current) => {
                                  const currentVisibleValues = getVisibleSelectedValues(current[filter.key], filter.options);
                                  const baseValues = areAllOptionsSelected(currentVisibleValues, filter.options)
                                    ? [...filter.options]
                                    : currentVisibleValues;
                                  const nextValues = baseValues.includes(option)
                                    ? baseValues.filter((value) => value !== option)
                                    : [...baseValues, option];

                                  return {
                                    ...current,
                                    [filter.key]: normalizeSelectedValues(nextValues, filter.options),
                                  };
                                });
                              }}
                            >
                              {formatBucketDisplayLabel(option)}
                            </button>
                            <button
                              type="button"
                              className={`runtime-subfilter-info-button ${floatingTooltip?.id === tooltipId ? 'is-open' : ''}`}
                              aria-label={`${formatBucketDisplayLabel(option)} 설명 보기`}
                              aria-expanded={floatingTooltip?.id === tooltipId}
                              onClick={(event) => {
                                event.preventDefault();
                                event.stopPropagation();
                                toggleFloatingTooltip(
                                  tooltipId,
                                  event.currentTarget,
                                  getBucketTooltipText(activeDbmsKey, filter.key, option)
                                );
                              }}
                            >
                              ?
                            </button>
                          </span>
                        );
                      })}
                    </div>
                  </div>
                );
              })}

            {selectedPlanSections.includes('hint') ? (
              <div className="runtime-subfilter-row">
                <span className="runtime-subfilter-label">Hint</span>
                <div className="runtime-subfilter-options">
                  {[
                    { key: 'ALL', label: '전체' },
                    { key: 'UNUSED', label: '미사용' },
                    { key: 'USED', label: '사용' },
                  ].map((option) => (
                    <button
                      key={option.key}
                      type="button"
                      className={`runtime-subfilter-button ${
                        option.key === 'ALL'
                          ? allHintFiltersSelected
                            ? 'is-selected'
                            : ''
                          : selectedHintFilters.includes(option.key as HintFilterValue)
                            ? 'is-selected'
                            : ''
                      }`}
                      aria-pressed={
                        option.key === 'ALL'
                          ? allHintFiltersSelected
                          : selectedHintFilters.includes(option.key as HintFilterValue)
                      }
                      onClick={(event) => {
                        event.preventDefault();
                        event.stopPropagation();
                        if (option.key === 'ALL') {
                          setSelectedHintFilters((current) =>
                            areAllOptionsSelected(current, HINT_FILTER_OPTIONS) ? [] : [...ALL_HINT_FILTERS]
                          );
                          return;
                        }

                        setSelectedHintFilters((current) => {
                          const nextValues = current.includes(option.key as HintFilterValue)
                            ? current.filter((value) => value !== option.key)
                            : [...current, option.key as HintFilterValue];

                          return normalizeSelectedValues(sortHintFilters(nextValues), HINT_FILTER_OPTIONS);
                        });
                      }}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </div>
            ) : null}
          </div>
        ) : null}

        {bucketModel ? (
          <div className="runtime-plot-shell">
            <div className="runtime-metric-column" role="group" aria-label="분포 축 선택">
              {METRIC_OPTIONS.map((metricOption) => {
                const isSelected = metricMode === metricOption.key;

                return (
                  <button
                    key={metricOption.key}
                    type="button"
                    className={`runtime-metric-button ${isSelected ? 'is-selected' : ''}`}
                    aria-pressed={isSelected}
                    onClick={(event) => {
                      event.preventDefault();
                      event.stopPropagation();
                      setMetricMode(metricOption.key);
                    }}
                  >
                    {metricOption.label}
                  </button>
                );
              })}
            </div>

            <div
              className="runtime-marker-column"
              style={{ gridTemplateRows: `repeat(${guideRowCount}, minmax(1rem, 1fr))` }}
            >
              {markersByRow.map((marker, rowIndex) => (
                <div key={`marker-row-${rowIndex}`} className="runtime-guide-row">
                  {marker ? (
                    <div className={`runtime-marker-item tooltip-anchor is-${marker.tone}`}>
                      <span className={`runtime-marker-token is-${marker.tone}`}>{marker.label}</span>
                      <span className={`ui-tooltip runtime-marker-tooltip${marker.key === 'median' ? ' is-passive' : ''}`}>
                        {renderMarkerTooltip(marker, metricMode, onSearchSelect)}
                      </span>
                    </div>
                  ) : null}
                </div>
              ))}
            </div>

            <div className="runtime-chart-stage">
              <div
                className="runtime-guide-grid"
                aria-hidden="true"
                style={{ gridTemplateRows: `repeat(${guideRowCount}, minmax(1rem, 1fr))` }}
              >
                {markersByRow.map((marker, rowIndex) => (
                  <div key={`guide-row-${rowIndex}`} className="runtime-guide-row">
                    {marker ? (
                      <span className={`runtime-connector-line is-${marker.tone}`}>
                        <span className="runtime-connector-gap" />
                        <span className="runtime-connector-track">
                          <span
                            className="runtime-connector-progress"
                            style={{ width: `max(0px, calc(${marker.targetPercent}% - var(--runtime-arrow-width)))` }}
                          />
                        </span>
                      </span>
                    ) : null}
                  </div>
                ))}
              </div>

              <div className="runtime-bars" style={{ gridTemplateColumns: `repeat(${bucketModel.buckets.length}, minmax(0, 1fr))` }}>
                {bucketModel.buckets.map((bucket, index) => {
                  const isMineBucket = markers.some(
                    (marker) =>
                      marker.key === 'mine' &&
                      marker.value >= bucket.startValue &&
                      marker.value < bucket.startValue + bucketModel.bucketSize
                  );
                  const axisLabel =
                    index === 0 ? firstAxisLabel : index === bucketModel.buckets.length - 1 ? lastAxisLabel : null;

                  return (
                    <div key={bucket.startValue} className={`runtime-bar-slot tooltip-anchor ${isMineBucket ? 'is-mine' : ''}`}>
                      <span
                        className={`runtime-bar ${isMineBucket ? 'is-mine' : ''}`}
                        style={{
                          height: bucket.count === 0 ? '0%' : `${Math.max((bucket.count / maxBucketCount) * 100, 9)}%`,
                        }}
                      />
                      {axisLabel ? <span className="runtime-axis-inline">{axisLabel}</span> : null}
                      <span className="ui-tooltip runtime-bar-tooltip">
                        <span className="ui-tooltip-title">
                          {formatBucketRange(bucket.startValue, bucketModel.bucketSize, metricMode)}
                        </span>
                        <span className="ui-tooltip-caption">{`${bucket.count}명`}</span>
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        ) : (
          <div className="runtime-empty-state">선택한 조건에 맞는 제출이 없습니다</div>
        )}
      </div>

      <aside className="runtime-stats-panel" aria-label="실행 통계">
        <div className="runtime-summary-row">
          <div className="runtime-summary-card">
            <p className="runtime-summary-title">실행시간(ms)</p>
            <div className="runtime-summary-grid">
              {[
                { label: '최소', value: timeSummary ? `${formatMetricAxisLabel(timeSummary.min, 'time')}` : '-' },
                { label: '평균', value: timeSummary ? `${Math.round(timeSummary.average * 10) / 10}` : '-' },
                { label: '중앙값', value: timeSummary ? `${Math.round(timeSummary.median * 10) / 10}` : '-' },
                {
                  label: '속도 편차',
                  value: timeSummary ? formatPercent(timeSummary.spreadRate) : '-',
                },
              ].map((item) => (
                <div key={item.label} className="runtime-summary-item">
                  <span className="runtime-summary-label">{item.label}</span>
                  <strong className="runtime-summary-value">{item.value}</strong>
                </div>
              ))}
            </div>
          </div>

          <div className="runtime-summary-card">
            <p className="runtime-summary-title">Scan Rows</p>
            <div className="runtime-summary-grid is-triple">
              {[
                { label: '최소', value: scanRowsSummary ? formatCount(scanRowsSummary.min) : '-' },
                { label: '평균', value: scanRowsSummary ? formatCount(scanRowsSummary.average) : '-' },
                { label: '중앙값', value: scanRowsSummary ? formatCount(scanRowsSummary.median) : '-' },
              ].map((item) => (
                <div key={item.label} className="runtime-summary-item">
                  <span className="runtime-summary-label">{item.label}</span>
                  <strong className="runtime-summary-value">{item.value}</strong>
                </div>
              ))}
            </div>
          </div>
        </div>

        {showPlanDetails && selectedRatioItems.length > 0 ? (
          <div className="runtime-stat-grid is-tuning">
            {selectedRatioItems.map((statItem) => (
              <StatItem
                key={statItem.id}
                label={statItem.label}
                value={statItem.value}
                detail={statItem.detail}
                tone={statItem.tone}
              />
            ))}
          </div>
        ) : null}
      </aside>

      {floatingTooltip
        ? createPortal(
            <div
              className="runtime-floating-tooltip"
              role="tooltip"
              style={{
                left: `${floatingTooltip.x}px`,
                top: `${floatingTooltip.y}px`,
              }}
            >
              {floatingTooltip.text}
            </div>,
            document.body
          )
        : null}
    </div>
  );
}




