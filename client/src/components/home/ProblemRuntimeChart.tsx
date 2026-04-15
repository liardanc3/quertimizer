import { createPortal } from 'react-dom';
import { useEffect, useMemo, useRef, useState, type MouseEvent } from 'react';
import { useMockSession } from '../../lib/session';
import type { AggregateBucket, DbmsType, FilterBucket, JoinBucket, ProblemSummary, ScanBucket, SortBucket } from '../../types/domain';
import './ProblemRuntimeChart.css';

const TARGET_BUCKET_COUNT = 40;
const MIN_VISUAL_BUCKET_COUNT = 12;
const FLOATING_TOOLTIP_DELAY_MS = 250;
const numberFormatter = new Intl.NumberFormat('ko-KR');

type MarkerKey = 'fastest' | 'mine';
type MarkerTone = 'fastest' | 'mine';
type FilterMatchMode = 'and' | 'or';
type RuntimeBucketFilterKey = 'scanBucket' | 'joinBucket' | 'filterBucket' | 'sortBucket' | 'aggregateBucket';
type BucketFilterValue = ScanBucket | JoinBucket | FilterBucket | SortBucket | AggregateBucket;
type PlanSectionKey = RuntimeBucketFilterKey | 'hint';
type HintFilterValue = 'USED' | 'UNUSED';

interface ProblemRuntimeChartProps {
  problem: ProblemSummary;
  onSearchSelect: (value: string) => void;
  onSolvedCountChange: (count: number) => void;
}

interface RuntimeSample {
  userId: string;
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
  userId?: string;
}

interface PlacedMarker extends RuntimeMarker {
  rowIndex: number;
  targetPercent: number;
}

interface BucketView {
  startValue: number;
  count: number;
}

interface BucketModel {
  bucketSize: number;
  buckets: BucketView[];
  minValue: number;
  maxValue: number;
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

interface FloatingTooltipState {
  id: string;
  text: string;
  x: number;
  y: number;
}

type BucketIndexMap = Record<RuntimeBucketFilterKey, Partial<Record<BucketFilterValue, number[]>>>;

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

const BUCKET_FILTERS_BY_DBMS: Record<DbmsType, BucketFilterDefinition[]> = {
  postgresql: [
    { key: 'scanBucket', label: 'Scan', options: ['FULL_SCAN', 'INDEX_SCAN', 'BITMAP_SCAN', 'TID_SCAN', 'DERIVED_SCAN', 'OTHERS'] },
    { key: 'joinBucket', label: 'Join', options: ['NONE', 'NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'OTHERS'] },
    { key: 'filterBucket', label: 'Filter', options: ['NONE', 'ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'OTHERS'] },
    { key: 'sortBucket', label: 'Sort', options: ['NONE', 'PLAIN_SORT', 'INCREMENTAL_SORT', 'OTHERS'] },
    { key: 'aggregateBucket', label: 'Aggregate', options: ['NONE', 'PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'MIXED_AGG', 'WINDOW_AGG', 'UNIQUE_AGG', 'SET_AGG', 'OTHERS'] },
  ],
  oracle: [
    { key: 'scanBucket', label: 'Scan', options: ['FULL_SCAN', 'ROWID_ACCESS', 'INDEX_SCAN', 'BITMAP_SCAN', 'DERIVED_SCAN', 'REMOTE_SCAN', 'OTHERS'] },
    { key: 'joinBucket', label: 'Join', options: ['NONE', 'NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'CARTESIAN_JOIN', 'OTHERS'] },
    { key: 'filterBucket', label: 'Filter', options: ['NONE', 'ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'OTHERS'] },
    { key: 'sortBucket', label: 'Sort', options: ['NONE', 'ORDER_SORT', 'GROUP_SORT', 'UNIQUE_SORT', 'WINDOW_SORT', 'OTHERS'] },
    { key: 'aggregateBucket', label: 'Aggregate', options: ['NONE', 'PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'WINDOW_AGG', 'OTHERS'] },
  ],
};

const DEFAULT_BUCKET_FILTERS: Record<RuntimeBucketFilterKey, BucketFilterValue[]> = {
  scanBucket: ['FULL_SCAN', 'ROWID_ACCESS', 'INDEX_SCAN', 'BITMAP_SCAN', 'TID_SCAN', 'DERIVED_SCAN', 'REMOTE_SCAN', 'OTHERS'],
  joinBucket: ['NONE', 'NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'CARTESIAN_JOIN', 'OTHERS'],
  filterBucket: ['NONE', 'ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'OTHERS'],
  sortBucket: ['NONE', 'PLAIN_SORT', 'INCREMENTAL_SORT', 'ORDER_SORT', 'GROUP_SORT', 'UNIQUE_SORT', 'WINDOW_SORT', 'OTHERS'],
  aggregateBucket: ['NONE', 'PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'MIXED_AGG', 'WINDOW_AGG', 'UNIQUE_AGG', 'SET_AGG', 'OTHERS'],
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

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function hasPlanElement(mask: number, index: number) {
  return (mask & (1 << index)) !== 0;
}

function hasAnyPlanElement(mask: number, indexes: number[]) {
  return indexes.some((index) => hasPlanElement(mask, index));
}

function formatMs(value: number) {
  return `${Math.round(value * 10) / 10}ms`;
}

function formatCostValue(value: number) {
  return String(Math.round(value * 10) / 10);
}

function formatPercent(value: number) {
  return `${numberFormatter.format(Math.round(value * 10) / 10)}%`;
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

function normalizeSelectedValues<T extends string>(selectedValues: T[], allOptions: readonly T[]) {
  return allOptions.filter((option) => selectedValues.includes(option));
}

function sortHintFilters(values: HintFilterValue[]) {
  return [...values].sort((left, right) => HINT_FILTER_DISPLAY_ORDER.indexOf(left) - HINT_FILTER_DISPLAY_ORDER.indexOf(right));
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
  return BUCKET_FILTERS_BY_DBMS[dbms];
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

function toSamples(problem: ProblemSummary, userId: string | null) {
  return (problem.submittedHistories ?? []).map((submittedHistory) => ({
    userId: submittedHistory.userId,
    dbms: submittedHistory.dbms,
    timeMs: submittedHistory.executionTimeMs,
    executionPlanElement: submittedHistory.executionPlanElement,
    isMine: userId != null && submittedHistory.userId === userId,
  }));
}

function buildBucketModel(samples: RuntimeSample[]): BucketModel | null {
  if (samples.length === 0) {
    return null;
  }

  const values = samples.map((sample) => sample.timeMs);
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const range = Math.max(1, maxValue - minValue + 1);
  const bucketSize = Math.max(1, Math.ceil(range / TARGET_BUCKET_COUNT));
  const bucketCount = Math.max(1, Math.ceil(range / bucketSize));
  const bucketStarts = Array.from({ length: bucketCount }, (_, index) => minValue + index * bucketSize);
  const lastBoundaryValue = minValue + bucketSize * bucketCount;
  const bucketCounts = new Map(bucketStarts.map((startValue) => [startValue, 0]));

  samples.forEach((sample) => {
    const normalizedValue = sample.timeMs === lastBoundaryValue ? sample.timeMs - 1 : sample.timeMs;
    const bucketStart = minValue + Math.floor((clamp(normalizedValue, minValue, lastBoundaryValue - 1) - minValue) / bucketSize) * bucketSize;
    bucketCounts.set(bucketStart, (bucketCounts.get(bucketStart) ?? 0) + 1);
  });

  return {
    bucketSize,
    buckets: bucketStarts.map((startValue) => ({ startValue, count: bucketCounts.get(startValue) ?? 0 })),
    minValue,
    maxValue,
  };
}

function buildDisplayBucketLayout(bucketModel: BucketModel): DisplayBucketLayout {
  const actualBucketCount = bucketModel.buckets.length;
  const displayBucketCount = Math.max(actualBucketCount, MIN_VISUAL_BUCKET_COUNT);

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

  const sortedSamples = [...samples].sort((left, right) => left.timeMs - right.timeMs || left.userId.localeCompare(right.userId));
  const markers: RuntimeMarker[] = [{ key: 'fastest', label: '1st', value: sortedSamples[0].timeMs, tone: 'fastest', userId: sortedSamples[0].userId }];
  const mySample = sortedSamples.find((sample) => sample.isMine);

  if (mySample) {
    markers.push({ key: 'mine', label: '내 기록', value: mySample.timeMs, tone: 'mine', userId: mySample.userId });
  }

  return markers.sort((left, right) => left.value - right.value || (left.key === 'fastest' ? -1 : 1));
}

function renderMarkerTooltip(marker: RuntimeMarker, onSearchSelect: (value: string) => void) {
  return (
    <>
      <span className="ui-tooltip-title">{formatMs(marker.value)}</span>
      {marker.userId ? (
        <button
          type="button"
          className="tooltip-link tooltip-link-inline"
          onClick={(event: MouseEvent<HTMLButtonElement>) => {
            event.preventDefault();
            event.stopPropagation();
            onSearchSelect(marker.userId ?? '');
          }}
        >
          {marker.userId}
        </button>
      ) : null}
    </>
  );
}

function buildPlanSectionRatioItems(args: {
  activeSamples: RuntimeSample[];
  dbms: DbmsType;
  filterMatchMode: FilterMatchMode;
  selectedPlanSections: PlanSectionKey[];
  selectedBucketFilters: Record<RuntimeBucketFilterKey, BucketFilterValue[]>;
  selectedHintFilters: HintFilterValue[];
}) {
  const { activeSamples, dbms, filterMatchMode, selectedPlanSections, selectedBucketFilters, selectedHintFilters } = args;
  const modeLabel = filterMatchMode.toUpperCase();
  const bucketItems = buildAvailableBucketFilters(dbms).flatMap((filter) => {
    if (!selectedPlanSections.includes(filter.key)) {
      return [];
    }

    const selectedValues = getVisibleSelectedValues(selectedBucketFilters[filter.key], filter.options);
    if (selectedValues.length === 0) {
      return [];
    }

    const isAllSelected = areAllOptionsSelected(selectedValues, filter.options);
    const ratio = isAllSelected ? 100 : calculatePercent(activeSamples, (sample) => selectedValues.some((value) => matchesBucketFilter(sample, dbms, filter.key, value)));
    return [{ id: filter.key, label: `${filter.label} 발생 비율 (${modeLabel})`, detail: isAllSelected ? '전체' : selectedValues.map((value) => formatBucketDisplayLabel(value)).join(', '), value: ratio === null ? '-' : formatPercent(ratio) }];
  });

  const hintItem =
    !selectedPlanSections.includes('hint') || selectedHintFilters.length === 0
      ? []
      : [{
          id: 'hint',
          label: `Hint 사용 비율 (${modeLabel})`,
          detail: areAllOptionsSelected(selectedHintFilters, HINT_FILTER_OPTIONS) ? '전체' : sortHintFilters(selectedHintFilters).map((value) => (value === 'USED' ? '사용' : '미사용')).join(', '),
          value: areAllOptionsSelected(selectedHintFilters, HINT_FILTER_OPTIONS)
            ? '100%'
            : formatPercent(calculatePercent(activeSamples, (sample) => selectedHintFilters.some((value) => value === 'USED' ? hasPlanElement(sample.executionPlanElement, 30) : !hasPlanElement(sample.executionPlanElement, 30))) ?? 0),
        }];

  return [...bucketItems, ...hintItem];
}

export default function ProblemRuntimeChart({ problem, onSearchSelect, onSolvedCountChange }: ProblemRuntimeChartProps) {
  const { userId, defaultDbms } = useMockSession();
  const samples = useMemo(() => toSamples(problem, userId), [problem, userId]);
  const availableDbms = useMemo(() => DBMS_OPTIONS.filter((option) => samples.some((sample) => sample.dbms === option.key)), [samples]);
  const defaultPlanSections = useMemo(() => PLAN_SECTION_OPTIONS.map((section) => section.key), []);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>(availableDbms[0]?.key ?? 'postgresql');
  const hasUserSelectedDbmsRef = useRef(false);
  const [filterMatchMode, setFilterMatchMode] = useState<FilterMatchMode>('or');
  const [selectedPlanSections, setSelectedPlanSections] = useState<PlanSectionKey[]>(defaultPlanSections);
  const [showPlanDetails, setShowPlanDetails] = useState(false);
  const [selectedBucketFilters, setSelectedBucketFilters] = useState<Record<RuntimeBucketFilterKey, BucketFilterValue[]>>(DEFAULT_BUCKET_FILTERS);
  const [selectedHintFilters, setSelectedHintFilters] = useState<HintFilterValue[]>(ALL_HINT_FILTERS);
  const [floatingTooltip, setFloatingTooltip] = useState<FloatingTooltipState | null>(null);
  const floatingTooltipTimerRef = useRef<number | null>(null);

  useEffect(() => {
    if (availableDbms.length === 0) {
      return;
    }

    if (!hasUserSelectedDbmsRef.current && defaultDbms && availableDbms.some((option) => option.key === defaultDbms) && selectedDbms !== defaultDbms) {
      setSelectedDbms(defaultDbms);
      return;
    }

    if (!availableDbms.some((option) => option.key === selectedDbms)) {
      if (defaultDbms && availableDbms.some((option) => option.key === defaultDbms)) {
        setSelectedDbms(defaultDbms);
        return;
      }

      setSelectedDbms(availableDbms[0].key);
    }
  }, [availableDbms, defaultDbms, selectedDbms]);

  const activeSamples = useMemo(() => samples.filter((sample) => sample.dbms === selectedDbms), [samples, selectedDbms]);
  const availableBucketFilters = useMemo(() => buildAvailableBucketFilters(selectedDbms), [selectedDbms]);
  const normalizedSelectedPlanSections = useMemo(
    () => defaultPlanSections.filter((sectionKey) => selectedPlanSections.includes(sectionKey)),
    [defaultPlanSections, selectedPlanSections]
  );
  const allPlanSectionsSelected = normalizedSelectedPlanSections.length === defaultPlanSections.length;
  const allHintFiltersSelected = areAllOptionsSelected(selectedHintFilters, HINT_FILTER_OPTIONS);
  const selectedBucketEntries = useMemo(
    () =>
      availableBucketFilters.flatMap((filter) => {
        if (!normalizedSelectedPlanSections.includes(filter.key)) {
          return [];
        }

        const selectedValues = getVisibleSelectedValues(selectedBucketFilters[filter.key], filter.options);
        if (selectedValues.length === 0 || areAllOptionsSelected(selectedValues, filter.options)) {
          return [];
        }

        return [[filter.key, selectedValues] as const];
      }),
    [availableBucketFilters, normalizedSelectedPlanSections, selectedBucketFilters]
  );
  const hasActiveHintSelection = normalizedSelectedPlanSections.includes('hint') && selectedHintFilters.length > 0 && !allHintFiltersSelected;
  const filteredSamples = useMemo(() => {
    if (selectedBucketEntries.length === 0 && !hasActiveHintSelection) {
      return activeSamples;
    }

    return activeSamples.filter((sample) => {
      const matches = [
        ...selectedBucketEntries.map(([filterKey, filterValues]) =>
          filterValues.some((filterValue) => matchesBucketFilter(sample, selectedDbms, filterKey, filterValue))
        ),
        ...(hasActiveHintSelection
          ? [
              selectedHintFilters.some((filterValue) =>
                filterValue === 'USED'
                  ? hasPlanElement(sample.executionPlanElement, 30)
                  : !hasPlanElement(sample.executionPlanElement, 30)
              ),
            ]
          : []),
      ];

      return matches.length === 0 ? true : filterMatchMode === 'and' ? matches.every(Boolean) : matches.some(Boolean);
    });
  }, [activeSamples, filterMatchMode, hasActiveHintSelection, selectedBucketEntries, selectedDbms, selectedHintFilters]);
  const bucketModel = useMemo(() => buildBucketModel(filteredSamples), [filteredSamples]);
  const displayBucketLayout = useMemo(() => (bucketModel ? buildDisplayBucketLayout(bucketModel) : null), [bucketModel]);
  const timeSummary = useMemo(() => buildTimeSummary(filteredSamples), [filteredSamples]);
  const markers = useMemo(() => buildMarkers(filteredSamples), [filteredSamples]);
  const maxBucketCount = bucketModel ? Math.max(1, ...bucketModel.buckets.map((bucket) => bucket.count)) : 1;
  const placedMarkers: PlacedMarker[] = useMemo(() => {
    if (bucketModel == null) {
      return [];
    }

    return markers.map((marker) => {
      const bucketIndex = clamp(
        Math.floor((marker.value - bucketModel.minValue) / bucketModel.bucketSize),
        0,
        bucketModel.buckets.length - 1
      );
      const displayBucketIndex = displayBucketLayout?.actualBucketIndexes[bucketIndex] ?? bucketIndex;
      const displayBucketCount = displayBucketLayout?.displayBuckets.length ?? bucketModel.buckets.length;
      return { ...marker, rowIndex: marker.key === 'mine' ? 0 : 1, targetPercent: ((displayBucketIndex + 0.5) / displayBucketCount) * 100 };
    });
  }, [bucketModel, displayBucketLayout, markers]);
  const markersByRow = useMemo(
    () => [placedMarkers.find((marker) => marker.key === 'mine') ?? null, placedMarkers.find((marker) => marker.key === 'fastest') ?? null],
    [placedMarkers]
  );
  const selectedRatioItems = useMemo(
    () =>
      buildPlanSectionRatioItems({
        activeSamples,
        dbms: selectedDbms,
        filterMatchMode,
        selectedPlanSections: normalizedSelectedPlanSections,
        selectedBucketFilters,
        selectedHintFilters,
      }),
    [activeSamples, filterMatchMode, normalizedSelectedPlanSections, selectedBucketFilters, selectedDbms, selectedHintFilters]
  );
  const firstAxisLabel = bucketModel ? String(Math.round(bucketModel.minValue)) : '';
  const lastAxisLabel = bucketModel ? String(Math.round(bucketModel.maxValue)) : '';
  const timeStatItems = timeSummary
    ? [
        { id: 'spread', label: `Cost \uD3B8\uCC28`, value: formatPercent(timeSummary.spreadRate) },
        { id: 'min', label: `\uCD5C\uC18C Cost`, value: formatCostValue(timeSummary.min) },
        { id: 'avg', label: `\uD3C9\uADE0 Cost`, value: formatCostValue(timeSummary.average) },
        { id: 'median', label: `Cost \uC911\uC559\uAC12`, value: formatCostValue(timeSummary.median) },
      ]
    : [];

  useEffect(() => {
    onSolvedCountChange(filteredSamples.length);
  }, [filteredSamples.length, onSolvedCountChange]);

  function clearFloatingTooltipTimer() {
    if (floatingTooltipTimerRef.current != null) {
      window.clearTimeout(floatingTooltipTimerRef.current);
      floatingTooltipTimerRef.current = null;
    }
  }

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

  function hideFloatingTooltip() {
    clearFloatingTooltipTimer();
    setFloatingTooltip(null);
  }

  useEffect(() => {
    return () => {
      clearFloatingTooltipTimer();
    };
  }, []);

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
              <div className="runtime-toolbar-group is-dbms">
                {availableDbms.map((option) => (
                  <button
                    key={option.key}
                    type="button"
                    className={`runtime-filter-button is-dbms ${selectedDbms === option.key ? 'is-selected' : ''}`}
                    aria-pressed={selectedDbms === option.key}
                    onClick={(event) => {
                      event.preventDefault();
                      event.stopPropagation();
                      hasUserSelectedDbmsRef.current = true;
                      setSelectedDbms(option.key);
                    }}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="runtime-filter-cluster" role="group" aria-label="필터 조합 방식">
              <div className="runtime-toolbar-group is-dbms">
                {FILTER_MODE_OPTIONS.map((option) => (
                  <button
                    key={option.key}
                    type="button"
                    className={`runtime-filter-button is-dbms ${filterMatchMode === option.key ? 'is-selected' : ''}`}
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
                  className={`runtime-filter-button is-plan-option ${allPlanSectionsSelected ? 'is-selected' : ''}`}
                  aria-pressed={allPlanSectionsSelected}
                  onClick={(event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    setSelectedPlanSections((current) => defaultPlanSections.every((sectionKey) => current.includes(sectionKey)) ? [] : defaultPlanSections);
                  }}
                >
                  전체
                </button>

                {PLAN_SECTION_OPTIONS.map((section) => {
                  const isSelected = normalizedSelectedPlanSections.includes(section.key);
                  const isVisuallySelected = !allPlanSectionsSelected && isSelected;

                  return (
                    <button
                      key={section.key}
                      type="button"
                      className={`runtime-filter-button is-plan-option ${isVisuallySelected ? 'is-selected' : ''}`}
                      aria-pressed={isSelected}
                      onClick={(event) => {
                        event.preventDefault();
                        event.stopPropagation();
                        setSelectedPlanSections((current) => {
                          const nextValues = current.includes(section.key) ? current.filter((currentKey) => currentKey !== section.key) : [...current, section.key];
                          return defaultPlanSections.filter((sectionKey) => nextValues.includes(sectionKey));
                        });
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
                const selectedValues = getVisibleSelectedValues(selectedBucketFilters[filter.key], filter.options);
                const isAllSelected = areAllOptionsSelected(selectedValues, filter.options);

                return (
                  <div key={filter.key} className="runtime-subfilter-row">
                    <span className="runtime-subfilter-label">{filter.label}</span>
                    <div className="runtime-subfilter-options is-bucket">
                      <button
                        type="button"
                        className={`runtime-subfilter-button runtime-subfilter-all-button ${isAllSelected ? 'is-selected' : ''}`}
                        aria-pressed={isAllSelected}
                        onClick={(event) => {
                          event.preventDefault();
                          event.stopPropagation();
                          setSelectedBucketFilters((current) => ({ ...current, [filter.key]: isAllSelected ? [] : [...filter.options] }));
                        }}
                      >
                        전체
                      </button>

                      <div className="runtime-subfilter-chip-grid">
                        {filter.options.map((option) => {
                          const tooltipId = `${filter.key}-${option}`;
                          const isSelected = selectedValues.includes(option);
                          const isVisuallySelected = !isAllSelected && isSelected;

                          return (
                            <span
                              key={option}
                              className={`runtime-subfilter-option ${isVisuallySelected ? 'is-selected' : ''}`}
                              onMouseEnter={(event) => {
                                scheduleFloatingTooltip(
                                  tooltipId,
                                  event.currentTarget,
                                  getBucketTooltipText(selectedDbms, filter.key, option)
                                );
                              }}
                              onMouseLeave={hideFloatingTooltip}
                            >
                              <button
                                type="button"
                                className="runtime-subfilter-button runtime-subfilter-button-plain"
                                aria-pressed={isSelected}
                                onClick={(event) => {
                                  event.preventDefault();
                                  event.stopPropagation();
                                  setSelectedBucketFilters((current) => {
                                    const currentVisibleValues = getVisibleSelectedValues(current[filter.key], filter.options);
                                    const baseValues = areAllOptionsSelected(currentVisibleValues, filter.options) ? [...filter.options] : currentVisibleValues;
                                    const nextValues = baseValues.includes(option) ? baseValues.filter((value) => value !== option) : [...baseValues, option];
                                    return { ...current, [filter.key]: normalizeSelectedValues(nextValues, filter.options) };
                                  });
                                }}
                              >
                                {formatBucketDisplayLabel(option)}
                              </button>
                            </span>
                          );
                        })}
                      </div>
                    </div>
                  </div>
                );
              })}

            {normalizedSelectedPlanSections.includes('hint') ? (
              <div className="runtime-subfilter-row">
                <span className="runtime-subfilter-label">Hint</span>
                <div className="runtime-subfilter-options">
                  {[
                    { key: 'ALL', label: '전체' },
                    { key: 'UNUSED', label: '미사용' },
                    { key: 'USED', label: '사용' },
                  ].map((option) => {
                    const isSelected = option.key === 'ALL' ? allHintFiltersSelected : selectedHintFilters.includes(option.key as HintFilterValue);
                    const isVisuallySelected = option.key === 'ALL' ? allHintFiltersSelected : !allHintFiltersSelected && isSelected;

                    return (
                      <button
                        key={option.key}
                        type="button"
                        className={`runtime-subfilter-button ${isVisuallySelected ? 'is-selected' : ''}`}
                        aria-pressed={isSelected}
                        onClick={(event) => {
                          event.preventDefault();
                          event.stopPropagation();

                          if (option.key === 'ALL') {
                            setSelectedHintFilters((current) => areAllOptionsSelected(current, HINT_FILTER_OPTIONS) ? [] : [...ALL_HINT_FILTERS]);
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
                    );
                  })}
                </div>
              </div>
            ) : null}
          </div>
        ) : null}

        {bucketModel ? (
          <div className="runtime-plot-shell">
            <div className="runtime-marker-column" style={{ gridTemplateRows: 'repeat(2, minmax(1rem, 1fr))' }}>
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
              <div className="runtime-guide-grid" aria-hidden="true" style={{ gridTemplateRows: 'repeat(2, minmax(1rem, 1fr))' }}>
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
                    markers.some((marker) => marker.key === 'mine' && marker.value >= bucket.startValue && marker.value < bucket.startValue + bucketModel.bucketSize);
                  const singleValueLabelIndex = displayBucketLayout?.actualBucketIndexes[0] ?? 0;
                  const axisLabel =
                    bucketModel.minValue === bucketModel.maxValue
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
                          <span className="ui-tooltip-title">{`${Math.round(bucket.startValue)}-${Math.round(bucket.startValue + bucketModel.bucketSize - 1)}ms`}</span>
                          <span className="ui-tooltip-caption">{`${bucket.count}명`}</span>
                        </span>
                      ) : null}
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

      <aside className="runtime-stats-panel" aria-label={`Cost \uD1B5\uACC4`}>
        <div className="runtime-stat-grid is-compact">
          {timeStatItems.map((item) => (
            <div key={item.id} className="runtime-stat-item is-neutral">
              <span className="runtime-stat-copy">
                <span className="runtime-stat-meta">
                  <span className="runtime-stat-label">{item.label}</span>
                </span>
              </span>
              <span className="runtime-stat-value">{item.value}</span>
            </div>
          ))}
        </div>

        {showPlanDetails && selectedRatioItems.length > 0 ? (
          <div className="runtime-stat-grid is-tuning">
            {selectedRatioItems.map((item) => (
              <div key={item.id} className="runtime-stat-item is-neutral">
                <span className="runtime-stat-copy">
                  <span className="runtime-stat-meta">
                    <span className="runtime-stat-label">{item.label}</span>
                    <span className="runtime-stat-detail">{item.detail}</span>
                  </span>
                </span>
                <span className="runtime-stat-value">{item.value}</span>
              </div>
            ))}
          </div>
        ) : null}
      </aside>

      {floatingTooltip ? createPortal(
        <div className="runtime-floating-tooltip" role="tooltip" style={{ left: `${floatingTooltip.x}px`, top: `${floatingTooltip.y}px` }}>
          {floatingTooltip.text}
        </div>,
        document.body
      ) : null}
    </div>
  );
}
