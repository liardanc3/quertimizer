import { createPortal } from 'react-dom';
import { Fragment, useCallback, useEffect, useMemo, useRef, useState, type MouseEvent } from 'react';
import { useMockSession } from '../../lib/session';
import { getUiText, getUiTextValue, useUiText } from '../../lib/uiText';
import type { AggregateBucket, DbmsType, FilterBucket, JoinBucket, ProblemSummary, ScanBucket, SortBucket } from '../../types/domain';
import './ProblemRuntimeChart.css';

const TARGET_BUCKET_COUNT = 40;
const MIN_VISUAL_BUCKET_COUNT = 12;
const FLOATING_TOOLTIP_DELAY_MS = 250;
const numberFormatter = new Intl.NumberFormat('ko-KR');
const costFormatter = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 2 });

type MarkerKey = 'fastest' | 'mine';
type MarkerTone = 'fastest' | 'mine';
type FilterMatchMode = 'and' | 'or';
type RuntimeBucketFilterKey = 'scanBucket' | 'joinBucket' | 'filterBucket' | 'sortBucket' | 'aggregateBucket';
type BucketFilterValue = ScanBucket | JoinBucket | FilterBucket | SortBucket | AggregateBucket;
type HintFilterValue = 'UNUSED' | 'USED';

interface ProblemRuntimeChartProps {
  problem: ProblemSummary;
  forcedDbms?: DbmsType;
  onSearchSelect: (value: string) => void;
}

interface RuntimeSample {
  handle: string;
  dbms: DbmsType;
  timeMs: number;
  executionPlanElement: number;
  isMine: boolean;
}

interface RuntimeMarker {
  key: MarkerKey;
  label: string;
  value: number;
  tone: MarkerTone;
  handle?: string;
}

interface PlacedMarker extends RuntimeMarker {
  rowIndex: number;
  targetPercent: number;
}

interface BucketView {
  startValue: number;
  endValue: number;
  count: number;
}

interface BucketModel {
  bucketSize: number;
  buckets: BucketView[];
  minValue: number;
  maxValue: number;
  axisMin: number;
  axisMax: number;
}

interface DisplayBucketLayout {
  displayBuckets: Array<BucketView | null>;
  actualBucketIndexes: number[];
}

interface BucketFilterDefinition {
  key: RuntimeBucketFilterKey;
  label: string;
  options: readonly BucketFilterValue[];
}

interface BucketFilterDefinitionSource {
  key: RuntimeBucketFilterKey;
  labelKey: string;
  options: readonly BucketFilterValue[];
}

interface FloatingTooltipState {
  id: string;
  text: string;
  x: number;
  y: number;
}

type BucketIndexMap = Record<RuntimeBucketFilterKey, Partial<Record<BucketFilterValue, number[]>>>;

const HINT_FILTER_OPTIONS: HintFilterValue[] = ['UNUSED', 'USED'];
const DEFAULT_HINT_FILTERS: HintFilterValue[] = [...HINT_FILTER_OPTIONS];

const BUCKET_FILTERS_BY_DBMS: Record<DbmsType, BucketFilterDefinitionSource[]> = {
  postgresql: [
    { key: 'scanBucket', labelKey: 'RUNTIME_SCAN_LABEL', options: ['FULL_SCAN', 'INDEX_SCAN', 'BITMAP_SCAN', 'TID_SCAN', 'DERIVED_SCAN', 'OTHERS'] },
    { key: 'joinBucket', labelKey: 'RUNTIME_JOIN_LABEL', options: ['NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'OTHERS'] },
    { key: 'filterBucket', labelKey: 'RUNTIME_FILTER_LABEL', options: ['ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'OTHERS'] },
    { key: 'sortBucket', labelKey: 'RUNTIME_SORT_LABEL', options: ['PLAIN_SORT', 'INCREMENTAL_SORT', 'OTHERS'] },
    { key: 'aggregateBucket', labelKey: 'RUNTIME_AGGREGATE_LABEL', options: ['PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'MIXED_AGG', 'WINDOW_AGG', 'UNIQUE_AGG', 'SET_AGG', 'OTHERS'] },
  ],
  oracle: [
    { key: 'scanBucket', labelKey: 'RUNTIME_SCAN_LABEL', options: ['FULL_SCAN', 'ROWID_ACCESS', 'INDEX_SCAN', 'BITMAP_SCAN', 'DERIVED_SCAN', 'REMOTE_SCAN', 'OTHERS'] },
    { key: 'joinBucket', labelKey: 'RUNTIME_JOIN_LABEL', options: ['NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'CARTESIAN_JOIN', 'OTHERS'] },
    { key: 'filterBucket', labelKey: 'RUNTIME_FILTER_LABEL', options: ['ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'OTHERS'] },
    { key: 'sortBucket', labelKey: 'RUNTIME_SORT_LABEL', options: ['ORDER_SORT', 'GROUP_SORT', 'UNIQUE_SORT', 'WINDOW_SORT', 'OTHERS'] },
    { key: 'aggregateBucket', labelKey: 'RUNTIME_AGGREGATE_LABEL', options: ['PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'WINDOW_AGG', 'OTHERS'] },
  ],
};

const DEFAULT_BUCKET_FILTERS: Record<RuntimeBucketFilterKey, BucketFilterValue[]> = {
  scanBucket: ['FULL_SCAN', 'ROWID_ACCESS', 'INDEX_SCAN', 'BITMAP_SCAN', 'TID_SCAN', 'DERIVED_SCAN', 'REMOTE_SCAN', 'OTHERS'],
  joinBucket: ['NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'CARTESIAN_JOIN', 'OTHERS'],
  filterBucket: ['ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'OTHERS'],
  sortBucket: ['PLAIN_SORT', 'INCREMENTAL_SORT', 'ORDER_SORT', 'GROUP_SORT', 'UNIQUE_SORT', 'WINDOW_SORT', 'OTHERS'],
  aggregateBucket: ['PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'MIXED_AGG', 'WINDOW_AGG', 'UNIQUE_AGG', 'SET_AGG', 'OTHERS'],
};

const BUCKET_PLAN_INDEXES_BY_DBMS: Record<DbmsType, BucketIndexMap> = {
  postgresql: {
    scanBucket: { FULL_SCAN: [0, 5], INDEX_SCAN: [1, 2], BITMAP_SCAN: [3, 4], TID_SCAN: [6], DERIVED_SCAN: [7, 8, 9, 10] },
    joinBucket: { NESTED_LOOP: [13], MERGE_JOIN: [12], HASH_JOIN: [11] },
    filterBucket: { ACCESS_FILTER: [29], POST_FILTER: [28] },
    sortBucket: { PLAIN_SORT: [16], INCREMENTAL_SORT: [17] },
    aggregateBucket: { GROUP_AGG: [15], HASH_AGG: [14], UNIQUE_AGG: [19] },
  },
  oracle: {
    scanBucket: { FULL_SCAN: [0], ROWID_ACCESS: [1], INDEX_SCAN: [2], BITMAP_SCAN: [3], DERIVED_SCAN: [4], REMOTE_SCAN: [5] },
    joinBucket: { NESTED_LOOP: [10], MERGE_JOIN: [11], HASH_JOIN: [12], CARTESIAN_JOIN: [13] },
    filterBucket: { ACCESS_FILTER: [14], POST_FILTER: [15], JOIN_FILTER: [16] },
    sortBucket: { ORDER_SORT: [17], GROUP_SORT: [18], UNIQUE_SORT: [19], WINDOW_SORT: [20] },
    aggregateBucket: { PLAIN_AGG: [21], GROUP_AGG: [22], HASH_AGG: [23], WINDOW_AGG: [24] },
  },
};

function SelectionCheckbox({ checked }: { checked: boolean }) {
  return <span className={`runtime-check-indicator ${checked ? 'is-checked' : ''}`} aria-hidden="true" />;
}

function FilterIcon() {
  return (
    <svg viewBox="0 0 18 18" fill="none" aria-hidden="true">
      <path d="M3.4 4.1h11.2l-4.4 5.05v3.65l-2.4 1.1V9.15L3.4 4.1Z" stroke="currentColor" strokeWidth="1.55" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 18 18" fill="none" aria-hidden="true">
      <path d="M4.8 4.8l8.4 8.4M13.2 4.8l-8.4 8.4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  );
}

function ApplyIcon() {
  return (
    <svg viewBox="0 0 18 18" fill="none" aria-hidden="true">
      <path d="M3.6 9.35l3.45 3.35 7.35-7.4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function hasPlanElement(mask: number, index: number) {
  return (mask & (1 << index)) !== 0;
}

function hasAnyPlanElement(mask: number, indexes: number[]) {
  return indexes.some((index) => hasPlanElement(mask, index));
}

function roundToPrecision(value: number, precision = 6) {
  const factor = 10 ** precision;
  return Math.round(value * factor) / factor;
}

function formatCostValue(value: number) {
  return costFormatter.format(Math.round(value * 100) / 100);
}

function formatPercent(value: number) {
  return `${numberFormatter.format(Math.round(value * 10) / 10)}%`;
}

function formatBucketDisplayLabel(value: BucketFilterValue) {
  return getUiTextValue(bucketValueToLabelKey(value), getBucketDisplayFallback(value));
}

function getBucketTooltipText(dbms: DbmsType, filterKey: RuntimeBucketFilterKey, value: BucketFilterValue) {
  const descriptionKey = bucketValueToDescriptionKey(filterKey, value);
  const fallback = getBucketTooltipFallback(dbms, filterKey, value);
  return getUiTextValue(descriptionKey, fallback);
}

function normalizeSelectedValues<T extends string>(selectedValues: T[], allOptions: readonly T[]) {
  return allOptions.filter((option) => selectedValues.includes(option));
}

function cloneBucketFilters(filters: Record<RuntimeBucketFilterKey, BucketFilterValue[]>) {
  return {
    scanBucket: [...filters.scanBucket],
    joinBucket: [...filters.joinBucket],
    filterBucket: [...filters.filterBucket],
    sortBucket: [...filters.sortBucket],
    aggregateBucket: [...filters.aggregateBucket],
  } satisfies Record<RuntimeBucketFilterKey, BucketFilterValue[]>;
}

function getVisibleSelectedValues<T extends string>(selectedValues: T[], allOptions: readonly T[]) {
  return allOptions.filter((option) => selectedValues.includes(option));
}

function areAllOptionsSelected<T extends string>(selectedValues: T[], allOptions: readonly T[]) {
  return allOptions.every((option) => selectedValues.includes(option));
}

function calculatePercent<T>(items: T[], predicate: (item: T) => boolean) {
  if (items.length === 0) {
    return null;
  }

  return (items.filter(predicate).length / items.length) * 100;
}

function buildAvailableBucketFilters(dbms: DbmsType) {
  return BUCKET_FILTERS_BY_DBMS[dbms].map((filter) => ({
    key: filter.key,
    label: getBucketSectionLabel(filter.labelKey),
    options: filter.options,
  }));
}

function getBucketSectionLabel(labelKey: string) {
  switch (labelKey) {
    case 'RUNTIME_SCAN_LABEL':
      return getUiTextValue(labelKey, 'Scan');
    case 'RUNTIME_JOIN_LABEL':
      return getUiTextValue(labelKey, 'Join');
    case 'RUNTIME_FILTER_LABEL':
      return getUiTextValue(labelKey, 'Filter');
    case 'RUNTIME_SORT_LABEL':
      return getUiTextValue(labelKey, 'Sort');
    case 'RUNTIME_AGGREGATE_LABEL':
      return getUiTextValue(labelKey, 'Aggregate');
    default:
      return labelKey;
  }
}

function bucketValueToLabelKey(value: BucketFilterValue) {
  switch (value) {
    case 'NONE':
      return 'COMMON_NONE_LABEL';
    case 'FULL_SCAN':
      return 'RUNTIME_FULL_SCAN_LABEL';
    case 'ROWID_ACCESS':
      return 'RUNTIME_ROWID_ACCESS_LABEL';
    case 'INDEX_SCAN':
      return 'RUNTIME_INDEX_SCAN_LABEL';
    case 'BITMAP_SCAN':
      return 'RUNTIME_BITMAP_SCAN_LABEL';
    case 'TID_SCAN':
      return 'RUNTIME_TID_SCAN_LABEL';
    case 'DERIVED_SCAN':
      return 'RUNTIME_DERIVED_SCAN_LABEL';
    case 'REMOTE_SCAN':
      return 'RUNTIME_REMOTE_SCAN_LABEL';
    case 'NESTED_LOOP':
      return 'RUNTIME_NESTED_LOOP_LABEL';
    case 'MERGE_JOIN':
      return 'RUNTIME_MERGE_JOIN_LABEL';
    case 'HASH_JOIN':
      return 'RUNTIME_HASH_JOIN_LABEL';
    case 'CARTESIAN_JOIN':
      return 'RUNTIME_CARTESIAN_JOIN_LABEL';
    case 'ACCESS_FILTER':
      return 'RUNTIME_ACCESS_FILTER_LABEL';
    case 'POST_FILTER':
      return 'RUNTIME_POST_FILTER_LABEL';
    case 'JOIN_FILTER':
      return 'RUNTIME_JOIN_FILTER_LABEL';
    case 'PLAIN_SORT':
      return 'RUNTIME_PLAIN_SORT_LABEL';
    case 'INCREMENTAL_SORT':
      return 'RUNTIME_INCREMENTAL_SORT_LABEL';
    case 'ORDER_SORT':
      return 'RUNTIME_ORDER_SORT_LABEL';
    case 'GROUP_SORT':
      return 'RUNTIME_GROUP_SORT_LABEL';
    case 'UNIQUE_SORT':
      return 'RUNTIME_UNIQUE_SORT_LABEL';
    case 'WINDOW_SORT':
      return 'RUNTIME_WINDOW_SORT_LABEL';
    case 'PLAIN_AGG':
      return 'RUNTIME_PLAIN_AGG_LABEL';
    case 'GROUP_AGG':
      return 'RUNTIME_GROUP_AGG_LABEL';
    case 'HASH_AGG':
      return 'RUNTIME_HASH_AGG_LABEL';
    case 'MIXED_AGG':
      return 'RUNTIME_MIXED_AGG_LABEL';
    case 'WINDOW_AGG':
      return 'RUNTIME_WINDOW_AGG_LABEL';
    case 'UNIQUE_AGG':
      return 'RUNTIME_UNIQUE_AGG_LABEL';
    case 'SET_AGG':
      return 'RUNTIME_SET_AGG_LABEL';
    case 'OTHERS':
      return 'RUNTIME_OTHERS_LABEL';
    default:
      return 'COMMON_NONE_LABEL';
  }
}

function getBucketDisplayFallback(value: BucketFilterValue) {
  if (value === 'NONE') {
    return '없음';
  }

  const normalizedSource = value.toLowerCase().endsWith('_agg') ? value.toLowerCase().replace(/_agg$/, '') : value.toLowerCase();
  const normalized = normalizedSource.replaceAll('_', ' ');
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function bucketValueToDescriptionKey(filterKey: RuntimeBucketFilterKey, value: BucketFilterValue) {
  if (value === 'NONE') {
    return 'RUNTIME_NONE_DESCRIPTION';
  }

  if (value === 'OTHERS') {
    return 'RUNTIME_OTHERS_DESCRIPTION';
  }

  switch (filterKey) {
    case 'scanBucket':
      switch (value) {
        case 'FULL_SCAN':
          return 'RUNTIME_FULL_SCAN_DESCRIPTION';
        case 'ROWID_ACCESS':
          return 'RUNTIME_ROWID_ACCESS_DESCRIPTION';
        case 'INDEX_SCAN':
          return 'RUNTIME_INDEX_SCAN_DESCRIPTION';
        case 'BITMAP_SCAN':
          return 'RUNTIME_BITMAP_SCAN_DESCRIPTION';
        case 'TID_SCAN':
          return 'RUNTIME_TID_SCAN_DESCRIPTION';
        case 'DERIVED_SCAN':
          return 'RUNTIME_DERIVED_SCAN_DESCRIPTION';
        case 'REMOTE_SCAN':
          return 'RUNTIME_REMOTE_SCAN_DESCRIPTION';
        default:
          return 'RUNTIME_DEFAULT_DESCRIPTION';
      }
    case 'joinBucket':
      switch (value) {
        case 'NESTED_LOOP':
          return 'RUNTIME_NESTED_LOOP_DESCRIPTION';
        case 'MERGE_JOIN':
          return 'RUNTIME_MERGE_JOIN_DESCRIPTION';
        case 'HASH_JOIN':
          return 'RUNTIME_HASH_JOIN_DESCRIPTION';
        default:
          return 'RUNTIME_DEFAULT_DESCRIPTION';
      }
    case 'filterBucket':
      switch (value) {
        case 'ACCESS_FILTER':
          return 'RUNTIME_ACCESS_FILTER_DESCRIPTION';
        case 'POST_FILTER':
          return 'RUNTIME_POST_FILTER_DESCRIPTION';
        case 'JOIN_FILTER':
          return 'RUNTIME_JOIN_FILTER_DESCRIPTION';
        default:
          return 'RUNTIME_DEFAULT_DESCRIPTION';
      }
    case 'sortBucket':
      switch (value) {
        case 'PLAIN_SORT':
          return 'RUNTIME_PLAIN_SORT_DESCRIPTION';
        case 'INCREMENTAL_SORT':
          return 'RUNTIME_INCREMENTAL_SORT_DESCRIPTION';
        default:
          return 'RUNTIME_DEFAULT_DESCRIPTION';
      }
    case 'aggregateBucket':
      switch (value) {
        case 'PLAIN_AGG':
          return 'RUNTIME_PLAIN_AGG_DESCRIPTION';
        case 'GROUP_AGG':
          return 'RUNTIME_GROUP_AGG_DESCRIPTION';
        case 'HASH_AGG':
          return 'RUNTIME_HASH_AGG_DESCRIPTION';
        case 'MIXED_AGG':
          return 'RUNTIME_MIXED_AGG_DESCRIPTION';
        case 'WINDOW_AGG':
          return 'RUNTIME_WINDOW_AGG_DESCRIPTION';
        case 'UNIQUE_AGG':
          return 'RUNTIME_UNIQUE_AGG_DESCRIPTION';
        case 'SET_AGG':
          return 'RUNTIME_SET_AGG_DESCRIPTION';
        default:
          return 'RUNTIME_DEFAULT_DESCRIPTION';
      }
    default:
      return 'RUNTIME_DEFAULT_DESCRIPTION';
  }
}

function getBucketTooltipFallback(dbms: DbmsType, filterKey: RuntimeBucketFilterKey, value: BucketFilterValue) {
  if (dbms === 'oracle') {
    if (filterKey === 'scanBucket' && value === 'FULL_SCAN') return 'TABLE ACCESS FULL 계열의 전체 읽기를 묶습니다.';
    if (filterKey === 'scanBucket' && value === 'ROWID_ACCESS') return 'ROWID를 따라 테이블 블록을 읽는 접근을 묶습니다.';
    if (filterKey === 'scanBucket' && value === 'INDEX_SCAN') return '인덱스를 통해 필요한 범위만 읽는 접근을 묶습니다.';
    if (filterKey === 'scanBucket' && value === 'BITMAP_SCAN') return '비트맵 기반 접근을 묶습니다.';
    if (filterKey === 'scanBucket' && value === 'DERIVED_SCAN') return 'VIEW, WITH, MATERIALIZE 같은 파생 결과 접근을 묶습니다.';
    if (filterKey === 'scanBucket' && value === 'REMOTE_SCAN') return '원격 객체를 읽는 접근을 묶습니다.';
  }

  if (filterKey === 'scanBucket' && value === 'FULL_SCAN') return '인덱스를 타지 않고 테이블 페이지를 순차적으로 읽는 경우를 묶습니다.';
  if (filterKey === 'scanBucket' && value === 'INDEX_SCAN') return 'Index Scan, Index Only Scan 계열을 묶습니다.';
  if (filterKey === 'scanBucket' && value === 'BITMAP_SCAN') return 'Bitmap Index Scan + Bitmap Heap Scan 계열을 묶습니다.';
  if (filterKey === 'scanBucket' && value === 'TID_SCAN') return 'Tid Scan 계열을 묶습니다.';
  if (filterKey === 'scanBucket' && value === 'DERIVED_SCAN') return 'Subquery, CTE, Function, Values 같은 파생 스캔을 묶습니다.';
  if (filterKey === 'joinBucket' && value === 'NESTED_LOOP') return '바깥 결과를 기준으로 안쪽 경로를 반복 탐색하는 조인입니다.';
  if (filterKey === 'joinBucket' && value === 'MERGE_JOIN') return '정렬된 두 입력을 병합하면서 조인하는 방식입니다.';
  if (filterKey === 'joinBucket' && value === 'HASH_JOIN') return '한쪽 입력으로 해시 테이블을 만들고 다른 쪽을 probe 하는 방식입니다.';
  if (filterKey === 'filterBucket' && value === 'ACCESS_FILTER') return '접근 단계에서 읽을 범위를 줄이는 조건을 묶습니다.';
  if (filterKey === 'filterBucket' && value === 'POST_FILTER') return '읽은 뒤 추가로 행을 거르는 조건을 묶습니다.';
  if (filterKey === 'filterBucket' && value === 'JOIN_FILTER') return '조인 과정에서 적용되는 조건을 묶습니다.';
  if (filterKey === 'sortBucket' && value === 'PLAIN_SORT') return '일반 Sort 계열을 묶습니다.';
  if (filterKey === 'sortBucket' && value === 'INCREMENTAL_SORT') return 'Incremental Sort 계열을 묶습니다.';
  if (filterKey === 'aggregateBucket' && value === 'PLAIN_AGG') return '그룹 키 없이 바로 집계하는 단순 집계를 묶습니다.';
  if (filterKey === 'aggregateBucket' && value === 'GROUP_AGG') return '정렬된 입력을 기준으로 그룹 집계를 수행하는 경우를 묶습니다.';
  if (filterKey === 'aggregateBucket' && value === 'HASH_AGG') return '해시 기반 그룹 집계를 묶습니다.';
  if (filterKey === 'aggregateBucket' && value === 'MIXED_AGG') return '해시와 정렬 전략이 섞인 집계 계열입니다.';
  if (filterKey === 'aggregateBucket' && value === 'WINDOW_AGG') return '윈도 함수 계산과 연결된 집계를 묶습니다.';
  if (filterKey === 'aggregateBucket' && value === 'UNIQUE_AGG') return '중복 제거 기반 집계를 묶습니다.';
  if (filterKey === 'aggregateBucket' && value === 'SET_AGG') return 'UNION, INTERSECT, EXCEPT 같은 집합 연산 기반 처리를 묶습니다.';
  if (value === 'NONE') return '해당 분류가 없는 실행 계획입니다.';
  if (value === 'OTHERS') return '위 분류로 명확히 묶기 어려운 기타 연산입니다.';
  return '실행 계획 설명입니다.';
}

function getSectionKnownIndexes(dbms: DbmsType, filterKey: RuntimeBucketFilterKey) {
  const filterIndexes = BUCKET_PLAN_INDEXES_BY_DBMS[dbms][filterKey];
  const knownIndexes = Object.entries(filterIndexes).filter(([value]) => value !== 'NONE' && value !== 'OTHERS').flatMap(([, indexes]) => indexes ?? []);
  return [...new Set(knownIndexes)];
}

function matchesBucketFilter(sample: RuntimeSample, dbms: DbmsType, filterKey: RuntimeBucketFilterKey, value: BucketFilterValue) {
  if (value === 'NONE') {
    return !hasAnyPlanElement(sample.executionPlanElement, getSectionKnownIndexes(dbms, filterKey));
  }

  return hasAnyPlanElement(sample.executionPlanElement, BUCKET_PLAN_INDEXES_BY_DBMS[dbms][filterKey][value] ?? []);
}

function matchesBucketFilterValues(sample: RuntimeSample, dbms: DbmsType, filterKey: RuntimeBucketFilterKey, values: BucketFilterValue[], matchMode: FilterMatchMode) {
  return matchMode === 'and'
    ? values.every((value) => matchesBucketFilter(sample, dbms, filterKey, value))
    : values.some((value) => matchesBucketFilter(sample, dbms, filterKey, value));
}

function matchesHintFilterValues(sample: RuntimeSample, values: HintFilterValue[], matchMode: FilterMatchMode) {
  return matchMode === 'and'
    ? values.every((value) => (value === 'USED' ? hasPlanElement(sample.executionPlanElement, 30) : !hasPlanElement(sample.executionPlanElement, 30)))
    : values.some((value) => (value === 'USED' ? hasPlanElement(sample.executionPlanElement, 30) : !hasPlanElement(sample.executionPlanElement, 30)));
}

function toSamples(problem: ProblemSummary, handle: string | null) {
  return (problem.submittedHistories ?? []).map((submittedHistory) => ({
    handle: submittedHistory.handle,
    dbms: submittedHistory.dbms,
    timeMs: typeof submittedHistory.cost === 'number' ? submittedHistory.cost : submittedHistory.executionTimeMs,
    executionPlanElement: submittedHistory.executionPlanElement,
    isMine: handle != null && submittedHistory.handle === handle,
  }));
}

function buildBucketModel(samples: RuntimeSample[]): BucketModel | null {
  if (samples.length === 0) {
    return null;
  }

  const values = samples.map((sample) => sample.timeMs);
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);

  if (minValue === maxValue) {
    return {
      bucketSize: 1,
      buckets: [{ startValue: minValue, endValue: maxValue, count: samples.length }],
      minValue,
      maxValue,
      axisMin: minValue,
      axisMax: maxValue,
    };
  }

  const paddedMinValue = minValue >= 0 ? 0 : minValue;
  const axisMin = Math.min(0, paddedMinValue);
  const axisMax = maxValue > axisMin ? maxValue : axisMin + 1;
  const bucketSize = roundToPrecision((axisMax - axisMin) / TARGET_BUCKET_COUNT);
  const buckets = Array.from({ length: TARGET_BUCKET_COUNT }, (_, index) => {
    const startValue = roundToPrecision(axisMin + bucketSize * index);
    const endValue = index === TARGET_BUCKET_COUNT - 1 ? axisMax : roundToPrecision(startValue + bucketSize);
    return { startValue, endValue, count: 0 };
  });

  samples.forEach((sample) => {
    const normalizedValue = clamp(sample.timeMs, axisMin, axisMax);
    const bucketIndex = normalizedValue === axisMax
      ? TARGET_BUCKET_COUNT - 1
      : clamp(Math.floor((normalizedValue - axisMin) / Math.max(bucketSize, 0.0001)), 0, TARGET_BUCKET_COUNT - 1);
    buckets[bucketIndex].count += 1;
  });

  return {
    bucketSize,
    buckets,
    minValue,
    maxValue,
    axisMin,
    axisMax,
  };
}

function buildDisplayBucketLayout(bucketModel: BucketModel): DisplayBucketLayout {
  const actualBucketCount = bucketModel.buckets.length;
  const displayBucketCount = actualBucketCount === 1 ? MIN_VISUAL_BUCKET_COUNT + 1 : Math.max(actualBucketCount, MIN_VISUAL_BUCKET_COUNT);

  if (actualBucketCount >= displayBucketCount) {
    return {
      displayBuckets: bucketModel.buckets,
      actualBucketIndexes: bucketModel.buckets.map((_, index) => index),
    };
  }

  const actualBucketIndexes: number[] = [];

  for (let index = 0; index < actualBucketCount; index += 1) {
    if (actualBucketCount === 1) {
      actualBucketIndexes.push(Math.floor(displayBucketCount / 2));
      continue;
    }

    const previousIndex = index > 0 ? actualBucketIndexes[index - 1] : -1;
    const remainingBucketCount = actualBucketCount - index - 1;
    const idealIndex = Math.round((index * (displayBucketCount - 1)) / (actualBucketCount - 1));
    const maximumIndex = displayBucketCount - remainingBucketCount - 1;
    actualBucketIndexes.push(Math.min(Math.max(idealIndex, previousIndex + 1), maximumIndex));
  }

  const displayBuckets = Array.from({ length: displayBucketCount }, () => null as BucketView | null);

  bucketModel.buckets.forEach((bucket, index) => {
    displayBuckets[actualBucketIndexes[index]] = bucket;
  });

  return {
    displayBuckets,
    actualBucketIndexes,
  };
}

function buildTimeSummary(samples: RuntimeSample[]) {
  if (samples.length === 0) {
    return null;
  }

  const values = [...samples.map((sample) => sample.timeMs)].sort((left, right) => left - right);
  const average = values.reduce((sum, value) => sum + value, 0) / values.length;
  const median = (values[Math.floor((values.length - 1) / 2)] + values[Math.floor(values.length / 2)]) / 2;
  const p90 = values[Math.max(0, Math.floor(values.length * 0.9) - 1)];
  return { min: values[0], average, median, spreadRate: ((p90 - values[0]) / Math.max(Math.abs(median), 1)) * 100 };
}

function buildMarkers(samples: RuntimeSample[]) {
  if (samples.length === 0) {
    return [];
  }

  const sortedSamples = [...samples].sort((left, right) => left.timeMs - right.timeMs || left.handle.localeCompare(right.handle));
  const markers: RuntimeMarker[] = [{
    key: 'fastest',
    label: getUiTextValue('RUNTIME_FASTEST_LABEL', '1st'),
    value: sortedSamples[0].timeMs,
    tone: 'fastest',
    handle: sortedSamples[0].handle,
  }];
  const mySample = sortedSamples.find((sample) => sample.isMine);

  if (mySample) {
    markers.push({
      key: 'mine',
      label: getUiTextValue('RUNTIME_MY_RECORD_LABEL', '내 기록'),
      value: mySample.timeMs,
      tone: 'mine',
      handle: mySample.handle,
    });
  }

  return markers.sort((left, right) => left.value - right.value || (left.key === 'fastest' ? -1 : 1));
}

function renderMarkerTooltip(marker: RuntimeMarker, onSearchSelect: (value: string) => void) {
  return (
    <>
      <span className="ui-tooltip-title">{formatCostValue(marker.value)}</span>
      {marker.handle ? (
        <button
          type="button"
          className="tooltip-link tooltip-link-inline"
          onClick={(event: MouseEvent<HTMLButtonElement>) => {
            event.preventDefault();
            event.stopPropagation();
            onSearchSelect(marker.handle ?? '');
          }}
        >
          {marker.handle}
        </button>
      ) : null}
    </>
  );
}

function buildPlanSectionRatioItems(args: {
  activeSamples: RuntimeSample[];
  availableBucketFilters: BucketFilterDefinition[];
  dbms: DbmsType;
  selectedBucketFilters: Record<RuntimeBucketFilterKey, BucketFilterValue[]>;
}) {
  const { activeSamples, availableBucketFilters, dbms, selectedBucketFilters } = args;
  const bucketItems = availableBucketFilters.map((filter) => {
    const selectedValues = getVisibleSelectedValues(selectedBucketFilters[filter.key], filter.options);
    const ratio = selectedValues.length === 0
      ? 0
      : calculatePercent(activeSamples, (sample) => selectedValues.some((value) => matchesBucketFilter(sample, dbms, filter.key, value))) ?? 0;

    return {
      id: filter.key,
      label: getUiText('RUNTIME_RATIO_SUFFIX_LABEL', { label: filter.label }, `${filter.label} 발생 비율`),
      value: formatPercent(ratio),
    };
  });

  const hintRatio = calculatePercent(activeSamples, (sample) => hasPlanElement(sample.executionPlanElement, 30)) ?? 0;

  return [
    ...bucketItems,
    { id: 'hint', label: getUiTextValue('RUNTIME_HINT_RATIO_LABEL', 'Hint 사용 비율'), value: formatPercent(hintRatio) },
  ];
}

export default function ProblemRuntimeChart({ problem, forcedDbms, onSearchSelect }: ProblemRuntimeChartProps) {
  const { text } = useUiText();
  const { handle, defaultDbms } = useMockSession();
  const samples = useMemo(() => toSamples(problem, handle), [problem, handle]);
  const runtimeDbmsOptions = useMemo(
    () => [
      { key: 'postgresql' as const, label: text('COMMON_POSTGRESQL_LABEL', 'PostgreSQL') },
      { key: 'oracle' as const, label: text('COMMON_ORACLE_LABEL', 'Oracle') },
    ],
    [text],
  );
  const runtimeFilterModeOptions = useMemo(
    () => [
      { key: 'and' as const, label: text('COMMON_AND_LABEL', 'AND') },
      { key: 'or' as const, label: text('COMMON_OR_LABEL', 'OR') },
    ],
    [text],
  );
  const runtimeHintFilterOptions = useMemo(
    () => HINT_FILTER_OPTIONS.map((option) => ({ key: option, label: option === 'USED' ? text('RUNTIME_USED_LABEL', '사용') : text('RUNTIME_UNUSED_LABEL', '미사용') })),
    [text],
  );
  const availableDbms = useMemo(() => {
    if (forcedDbms) {
      return runtimeDbmsOptions.filter((option) => option.key === forcedDbms);
    }

    const sampleDbms = runtimeDbmsOptions.filter((option) => samples.some((sample) => sample.dbms === option.key));
    return sampleDbms.length > 0 ? sampleDbms : runtimeDbmsOptions;
  }, [forcedDbms, runtimeDbmsOptions, samples]);
  const selectedDbms = useMemo(() => {
    if (forcedDbms) {
      return forcedDbms;
    }

    if (defaultDbms && availableDbms.some((option) => option.key === defaultDbms)) {
      return defaultDbms;
    }

    return availableDbms[0]?.key ?? 'postgresql';
  }, [availableDbms, defaultDbms, forcedDbms]);
  const [filterMatchMode, setFilterMatchMode] = useState<FilterMatchMode>('or');
  const [selectedBucketFilters, setSelectedBucketFilters] = useState<Record<RuntimeBucketFilterKey, BucketFilterValue[]>>(
    () => cloneBucketFilters(DEFAULT_BUCKET_FILTERS)
  );
  const [selectedHintFilters, setSelectedHintFilters] = useState<HintFilterValue[]>(() => [...DEFAULT_HINT_FILTERS]);
  const [draftFilterMatchMode, setDraftFilterMatchMode] = useState<FilterMatchMode>('or');
  const [draftBucketFilters, setDraftBucketFilters] = useState<Record<RuntimeBucketFilterKey, BucketFilterValue[]>>(
    () => cloneBucketFilters(DEFAULT_BUCKET_FILTERS)
  );
  const [draftHintFilters, setDraftHintFilters] = useState<HintFilterValue[]>(() => [...DEFAULT_HINT_FILTERS]);
  const [isFilterPopoverOpen, setIsFilterPopoverOpen] = useState(false);
  const [hasSeenFilterPopover, setHasSeenFilterPopover] = useState(false);
  const [floatingTooltip, setFloatingTooltip] = useState<FloatingTooltipState | null>(null);
  const floatingTooltipTimerRef = useRef<number | null>(null);

  const activeSamples = useMemo(() => samples.filter((sample) => sample.dbms === selectedDbms), [samples, selectedDbms]);
  const availableBucketFilters = useMemo(() => buildAvailableBucketFilters(selectedDbms), [selectedDbms]);
  const selectedBucketEntries = useMemo(
    () =>
      availableBucketFilters.flatMap((filter) => {
        const selectedValues = getVisibleSelectedValues(selectedBucketFilters[filter.key], filter.options);
        if (selectedValues.length === 0) {
          return [];
        }

        return [[filter.key, selectedValues] as const];
      }),
    [availableBucketFilters, selectedBucketFilters]
  );
  const emptyBucketFilterKeys = useMemo(
    () =>
      availableBucketFilters.flatMap((filter) =>
        getVisibleSelectedValues(selectedBucketFilters[filter.key], filter.options).length === 0 ? [filter.key] : []
      ),
    [availableBucketFilters, selectedBucketFilters]
  );
  const hasActiveHintSelection = selectedHintFilters.length > 0;
  const hasExplicitPlanFilter = selectedBucketEntries.length > 0 || emptyBucketFilterKeys.length > 0 || hasActiveHintSelection;
  const filteredSamples = useMemo(() => {
    if (!hasExplicitPlanFilter) {
      return activeSamples;
    }

    return activeSamples.filter((sample) => {
      const matches = [
        ...selectedBucketEntries.map(([filterKey, filterValues]) =>
          matchesBucketFilterValues(sample, selectedDbms, filterKey, filterValues, filterMatchMode)
        ),
        ...(filterMatchMode === 'and'
          ? emptyBucketFilterKeys.map((filterKey) => matchesBucketFilter(sample, selectedDbms, filterKey, 'NONE'))
          : []),
        ...(hasActiveHintSelection
          ? [matchesHintFilterValues(sample, selectedHintFilters, filterMatchMode)]
          : []),
      ];

      return matches.length === 0 ? false : filterMatchMode === 'and' ? matches.every(Boolean) : matches.some(Boolean);
    });
  }, [activeSamples, emptyBucketFilterKeys, filterMatchMode, hasActiveHintSelection, hasExplicitPlanFilter, selectedBucketEntries, selectedDbms, selectedHintFilters]);
  const bucketModel = useMemo(() => buildBucketModel(filteredSamples), [filteredSamples]);
  const displayBucketLayout = useMemo(() => (bucketModel ? buildDisplayBucketLayout(bucketModel) : null), [bucketModel]);
  const timeSummary = useMemo(() => buildTimeSummary(filteredSamples), [filteredSamples]);
  const markers = useMemo(() => buildMarkers(filteredSamples), [filteredSamples]);
  const hasActivePlanFilter = hasExplicitPlanFilter;
  const maxBucketCount = bucketModel ? Math.max(1, ...bucketModel.buckets.map((bucket) => bucket.count)) : 1;
  const placedMarkers: PlacedMarker[] = useMemo(() => {
    if (bucketModel == null) {
      return [];
    }

    return markers.map((marker) => {
      const axisRange = Math.max(bucketModel.axisMax - bucketModel.axisMin, 0.0001);
      const bucketIndex = clamp(
        Math.floor((clamp(marker.value, bucketModel.axisMin, bucketModel.axisMax) - bucketModel.axisMin) / Math.max(bucketModel.bucketSize, 0.0001)),
        0,
        bucketModel.buckets.length - 1
      );
      const displayBucketIndex = displayBucketLayout?.actualBucketIndexes[bucketIndex] ?? bucketIndex;
      const displayBucketCount = displayBucketLayout?.displayBuckets.length ?? bucketModel.buckets.length;

      return {
        ...marker,
        rowIndex: marker.key === 'mine' ? 0 : 1,
        targetPercent: displayBucketLayout
          ? ((displayBucketIndex + 0.5) / displayBucketCount) * 100
          : ((clamp(marker.value, bucketModel.axisMin, bucketModel.axisMax) - bucketModel.axisMin) / axisRange) * 100,
      };
    });
  }, [bucketModel, displayBucketLayout, markers]);
  const markersByRow = useMemo(
    () => [placedMarkers.find((marker) => marker.key === 'mine') ?? null, placedMarkers.find((marker) => marker.key === 'fastest') ?? null],
    [placedMarkers]
  );
  const markerAxisLabels = useMemo(() => {
    const labelsByValue = new Map<string, { key: string; label: string; targetPercent: number }>();
    placedMarkers.forEach((marker) => {
      const label = formatCostValue(marker.value);
      if (!labelsByValue.has(label)) {
        labelsByValue.set(label, { key: marker.key, label, targetPercent: marker.targetPercent });
      }
    });

    return [...labelsByValue.values()].slice(0, 2);
  }, [placedMarkers]);
  const selectedRatioItems = useMemo(
    () =>
      buildPlanSectionRatioItems({
        activeSamples,
        availableBucketFilters,
        dbms: selectedDbms,
        selectedBucketFilters,
      }),
    [activeSamples, availableBucketFilters, selectedBucketFilters, selectedDbms]
  );
  const costMetricItems = [
    { id: 'sample-count', label: text('RUNTIME_SAMPLE_COUNT_LABEL', '집계 수'), value: numberFormatter.format(filteredSamples.length) },
    { id: 'avg', label: text('RUNTIME_AVERAGE_COST_LABEL', '평균 Cost'), value: timeSummary ? formatCostValue(timeSummary.average) : '-' },
    { id: 'min', label: text('RUNTIME_MINIMUM_COST_LABEL', '최소 Cost'), value: timeSummary ? formatCostValue(timeSummary.min) : '-' },
    { id: 'median', label: text('RUNTIME_MEDIAN_COST_LABEL', 'Cost 중앙값'), value: timeSummary ? formatCostValue(timeSummary.median) : '-' },
  ];
  const firstAxisLabel = bucketModel ? formatCostValue(bucketModel.axisMin) : '';
  const lastAxisLabel = bucketModel ? formatCostValue(bucketModel.axisMax) : '';

  const clearFloatingTooltipTimer = useCallback(() => {
    if (floatingTooltipTimerRef.current != null) {
      window.clearTimeout(floatingTooltipTimerRef.current);
      floatingTooltipTimerRef.current = null;
    }
  }, []);

  function showFloatingTooltip(id: string, anchor: HTMLElement, text: string) {
    const rect = anchor.getBoundingClientRect();
    setFloatingTooltip({ id, text, x: rect.left + rect.width / 2, y: rect.top - 10 });
  }

  function scheduleFloatingTooltip(id: string, anchor: HTMLElement, text: string) {
    clearFloatingTooltipTimer();
    floatingTooltipTimerRef.current = window.setTimeout(() => {
      showFloatingTooltip(id, anchor, text);
      floatingTooltipTimerRef.current = null;
    }, FLOATING_TOOLTIP_DELAY_MS);
  }

  const hideFloatingTooltip = useCallback(() => {
    clearFloatingTooltipTimer();
    setFloatingTooltip(null);
  }, [clearFloatingTooltipTimer]);

  function openFilterPopover() {
    hideFloatingTooltip();
    setHasSeenFilterPopover(true);
    setDraftFilterMatchMode(filterMatchMode);
    setDraftBucketFilters(cloneBucketFilters(selectedBucketFilters));
    setDraftHintFilters([...selectedHintFilters]);
    setIsFilterPopoverOpen(true);
  }

  function cancelFilterPopover() {
    hideFloatingTooltip();
    setDraftFilterMatchMode(filterMatchMode);
    setDraftBucketFilters(cloneBucketFilters(selectedBucketFilters));
    setDraftHintFilters([...selectedHintFilters]);
    setIsFilterPopoverOpen(false);
  }

  function applyFilterPopover() {
    hideFloatingTooltip();
    setFilterMatchMode(draftFilterMatchMode);
    setSelectedBucketFilters(cloneBucketFilters(draftBucketFilters));
    setSelectedHintFilters([...draftHintFilters]);
    setIsFilterPopoverOpen(false);
  }

  useEffect(() => {
    return () => {
      clearFloatingTooltipTimer();
    };
  }, [clearFloatingTooltipTimer]);

  useEffect(() => {
    if (floatingTooltip == null) {
      return undefined;
    }

    window.addEventListener('resize', hideFloatingTooltip);
    window.addEventListener('scroll', hideFloatingTooltip, true);

    return () => {
      window.removeEventListener('resize', hideFloatingTooltip);
      window.removeEventListener('scroll', hideFloatingTooltip, true);
    };
  }, [floatingTooltip, hideFloatingTooltip]);

  return (
    <div className={`problem-runtime-shell ${isFilterPopoverOpen ? 'is-filter-popover-open' : ''}`.trim()}>
      <div className="runtime-chart-panel-head">
        <button
          type="button"
          className={`runtime-filter-launch ${hasActivePlanFilter ? 'is-active' : ''} ${!hasSeenFilterPopover && !isFilterPopoverOpen ? 'is-hinting' : ''}`.trim()}
          aria-label={text('RUNTIME_FILTER_OPEN_LABEL', '실행 계획 요소 열기')}
          onClick={(event) => {
            event.preventDefault();
            event.stopPropagation();
            if (!isFilterPopoverOpen) {
              openFilterPopover();
            }
          }}
        >
          <FilterIcon />
        </button>
      </div>

      <div className={`runtime-content ${isFilterPopoverOpen ? 'is-filter-blurred' : ''}`.trim()}>
        {bucketModel ? (
          <section className="runtime-summary-panel" aria-label={text('RUNTIME_SUMMARY_PANEL_LABEL', '통계 개요')}>
            <div className="runtime-metric-grid">
              <div className="runtime-metric-row is-cost">
                {costMetricItems.map((item) => (
                  <div key={item.id} className="runtime-metric-cell">
                    <span className="runtime-metric-label">{item.label}</span>
                    <span className="runtime-metric-value">{item.value}</span>
                  </div>
                ))}
              </div>
              <div className="runtime-metric-row is-plan">
                {selectedRatioItems.map((item) => (
                  <div key={item.id} className="runtime-metric-cell">
                    <span className="runtime-metric-label">{item.label}</span>
                    <span className="runtime-metric-value">{item.value}</span>
                  </div>
                ))}
              </div>
            </div>
          </section>
        ) : null}

        <section className="runtime-chart-panel" aria-label={text('RUNTIME_COST_DISTRIBUTION_LABEL', 'Cost 분포')}>
          {bucketModel ? (
            <div className="runtime-plot-shell">
              <div className="runtime-marker-column" style={{ gridTemplateRows: 'repeat(2, minmax(0.92rem, 1fr))' }}>
                {markersByRow.map((marker, rowIndex) => (
                  <div key={`marker-row-${rowIndex}`} className="runtime-guide-row">
                    {marker ? (
                      <div className={`runtime-marker-item tooltip-anchor is-${marker.tone}`}>
                        <span className={`runtime-marker-token is-${marker.tone}`}>{marker.label}</span>
                        <span className="ui-tooltip runtime-marker-tooltip">{renderMarkerTooltip(marker, onSearchSelect)}</span>
                      </div>
                    ) : null}
                  </div>
                ))}
              </div>

              <div className="runtime-chart-stage">
                <div className="runtime-guide-grid" aria-hidden="true" style={{ gridTemplateRows: 'repeat(2, minmax(0.92rem, 1fr))' }}>
                  {markersByRow.map((marker, rowIndex) => (
                    <div key={`guide-row-${rowIndex}`} className="runtime-guide-row">
                      {marker ? (
                        <span className={`runtime-connector-line is-${marker.tone}`}>
                          <span className="runtime-connector-gap" />
                          <span className="runtime-connector-track">
                            <span className="runtime-connector-progress" style={{ width: `max(0px, calc(${marker.targetPercent}% - var(--runtime-arrow-width)))` }} />
                          </span>
                        </span>
                      ) : null}
                    </div>
                  ))}
                </div>

                <div className="runtime-bars" style={{ gridTemplateColumns: `repeat(${displayBucketLayout?.displayBuckets.length ?? bucketModel.buckets.length}, minmax(0, 1fr))` }}>
                  {(displayBucketLayout?.displayBuckets ?? bucketModel.buckets).map((bucket, index) => {
                    const isMineBucket =
                      bucket != null &&
                      markers.some(
                        (marker) =>
                          marker.key === 'mine' &&
                          marker.value >= bucket.startValue &&
                          (marker.value < bucket.endValue || bucket.endValue === bucketModel.axisMax)
                      );
                    const singleValueLabelIndex = displayBucketLayout?.actualBucketIndexes[0] ?? 0;
                    const axisLabel =
                      bucketModel.axisMin === bucketModel.axisMax
                        ? index === singleValueLabelIndex
                          ? firstAxisLabel
                          : null
                        : index === 0
                          ? firstAxisLabel
                          : index === (displayBucketLayout?.displayBuckets.length ?? bucketModel.buckets.length) - 1
                            ? lastAxisLabel
                            : null;

                    return (
                      <div key={bucket ? bucket.startValue : `empty-${index}`} className={`runtime-bar-slot tooltip-anchor ${isMineBucket ? 'is-mine' : ''}`}>
                        <span
                          className={`runtime-bar ${isMineBucket ? 'is-mine' : ''}`}
                          style={{ height: bucket == null || bucket.count === 0 ? '0%' : `${Math.max((bucket.count / maxBucketCount) * 100, 9)}%` }}
                        />
                        {axisLabel ? <span className="runtime-axis-inline">{axisLabel}</span> : null}
                        {bucket ? (
                          <span className="ui-tooltip runtime-bar-tooltip">
                            <span className="ui-tooltip-title">{`${formatCostValue(bucket.startValue)} - ${formatCostValue(bucket.endValue)}`}</span>
                            <span className="ui-tooltip-caption">{text('RUNTIME_BUCKET_COUNT_LABEL', { count: bucket.count }, `${bucket.count}명`)}</span>
                          </span>
                        ) : null}
                      </div>
                    );
                  })}
                  {markerAxisLabels.map((marker) => (
                    <span
                      key={`marker-axis-${marker.key}`}
                      className="runtime-axis-inline"
                      style={{ left: `${marker.targetPercent}%` }}
                      aria-hidden="true"
                    >
                      {marker.label}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          ) : <div className="runtime-empty-state">{text('RUNTIME_EMPTY_STATE', '선택한 조건에 맞는 제출이 없습니다.')}</div>}
        </section>
      </div>

      {isFilterPopoverOpen ? (
        <div
          className="runtime-filter-overlay"
          role="presentation"
        >
          <div
            className="runtime-filter-popover"
            role="dialog"
            aria-modal="true"
            aria-label={text('RUNTIME_FILTER_TITLE', '실행 계획 요소')}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="runtime-filter-popover-head">
              <strong>{text('RUNTIME_FILTER_TITLE', '실행 계획 요소')}</strong>
              <div className="runtime-mode-toggle" role="group" aria-label={text('RUNTIME_FILTER_MATCH_MODE_LABEL', '필터 조합 방식')}>
                {runtimeFilterModeOptions.map((option, index) => (
                  <Fragment key={option.key}>
                    {index > 0 ? <span className="runtime-mode-divider" aria-hidden="true">/</span> : null}
                    <button
                      type="button"
                      className={`runtime-mode-button ${draftFilterMatchMode === option.key ? 'is-selected' : ''}`}
                      aria-pressed={draftFilterMatchMode === option.key}
                      onClick={(event) => {
                        event.preventDefault();
                        event.stopPropagation();
                        setDraftFilterMatchMode(option.key);
                      }}
                    >
                      {option.label}
                    </button>
                  </Fragment>
                ))}
              </div>
              <div className="runtime-filter-popover-actions">
                <button
                  type="button"
                  className="runtime-filter-icon-button is-cancel"
                  aria-label={text('RUNTIME_FILTER_CANCEL_LABEL', '실행 계획 요소 필터 취소')}
                  onClick={(event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    cancelFilterPopover();
                  }}
                >
                  <CloseIcon />
                </button>
                <button
                  type="button"
                  className="runtime-filter-icon-button is-apply"
                  aria-label={text('RUNTIME_FILTER_APPLY_LABEL', '실행 계획 요소 필터 적용')}
                  onClick={(event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    applyFilterPopover();
                  }}
                >
                  <ApplyIcon />
                </button>
              </div>
            </div>

            <div
              className="runtime-subfilter-board runtime-plan-shell-panel"
              role="group"
              aria-label={text('RUNTIME_FILTER_DETAIL_GROUP_LABEL', '실행 계획 요소 세부 선택')}
            >
              {availableBucketFilters.map((filter) => {
                const selectedValues = getVisibleSelectedValues(draftBucketFilters[filter.key], filter.options);
                const isAllSelected = areAllOptionsSelected(selectedValues, filter.options);

                return (
                  <div key={filter.key} className="runtime-subfilter-row">
                    <span className="runtime-subfilter-label">{filter.label}</span>
                    <div className="runtime-subfilter-options is-bucket">
                      <button
                        type="button"
                        className={`runtime-subfilter-button runtime-subfilter-all-button runtime-check-button ${isAllSelected ? 'is-selected' : ''}`}
                        aria-pressed={isAllSelected}
                        onClick={(event) => {
                          event.preventDefault();
                          event.stopPropagation();
                          setDraftBucketFilters((current) => ({ ...current, [filter.key]: isAllSelected ? [] : [...filter.options] }));
                        }}
                      >
                        <SelectionCheckbox checked={isAllSelected} />
                        <span className="runtime-check-label">{text('RUNTIME_ALL_LABEL', '전체')}</span>
                      </button>

                      <div className="runtime-subfilter-chip-grid">
                        {filter.options.map((option) => {
                          const tooltipId = `${filter.key}-${option}`;
                          const isSelected = selectedValues.includes(option);

                          return (
                            <span
                              key={option}
                              className="runtime-subfilter-option"
                              onMouseEnter={(event) => {
                                scheduleFloatingTooltip(tooltipId, event.currentTarget, getBucketTooltipText(selectedDbms, filter.key, option));
                              }}
                              onMouseLeave={hideFloatingTooltip}
                            >
                              <button
                                type="button"
                                className={`runtime-subfilter-button runtime-subfilter-button-plain runtime-check-button ${isSelected ? 'is-selected' : ''}`}
                                aria-pressed={isSelected}
                                onClick={(event) => {
                                  event.preventDefault();
                                  event.stopPropagation();
                                  setDraftBucketFilters((current) => {
                                    const currentVisibleValues = getVisibleSelectedValues(current[filter.key], filter.options);
                                    const isCurrentAllSelected = areAllOptionsSelected(currentVisibleValues, filter.options);
                                    const baseValues = isCurrentAllSelected ? [] : currentVisibleValues;
                                    const nextValues = baseValues.includes(option) ? baseValues.filter((value) => value !== option) : [...baseValues, option];
                                    return { ...current, [filter.key]: normalizeSelectedValues(nextValues, filter.options) };
                                  });
                                }}
                              >
                                <SelectionCheckbox checked={isSelected} />
                                <span className="runtime-check-label">{formatBucketDisplayLabel(option)}</span>
                              </button>
                            </span>
                          );
                        })}
                      </div>
                    </div>
                  </div>
                );
              })}

              <div className="runtime-subfilter-row">
                <span className="runtime-subfilter-label">{text('COMMON_HINT_LABEL', 'Hint')}</span>
                <div className="runtime-subfilter-options is-bucket">
                  <div className="runtime-subfilter-chip-grid">
                    {runtimeHintFilterOptions.map((option) => {
                      const isSelected = draftHintFilters.includes(option.key);

                      return (
                        <span key={option.key} className="runtime-subfilter-option">
                          <button
                            type="button"
                            className={`runtime-subfilter-button runtime-subfilter-button-plain runtime-check-button ${isSelected ? 'is-selected' : ''}`}
                            aria-pressed={isSelected}
                            onClick={(event) => {
                              event.preventDefault();
                              event.stopPropagation();
                              setDraftHintFilters((current) =>
                                current.includes(option.key) ? current.filter((value) => value !== option.key) : [...current, option.key]
                              );
                            }}
                          >
                            <SelectionCheckbox checked={isSelected} />
                            <span className="runtime-check-label">{option.label}</span>
                          </button>
                        </span>
                      );
                    })}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {floatingTooltip ? createPortal(
        <div className="runtime-floating-tooltip" role="tooltip" style={{ left: `${floatingTooltip.x}px`, top: `${floatingTooltip.y}px` }}>
          {floatingTooltip.text}
        </div>,
        document.body
      ) : null}
    </div>
  );
}
