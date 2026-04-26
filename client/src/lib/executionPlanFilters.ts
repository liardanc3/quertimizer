import type {
  AggregateBucket,
  DbmsType,
  FilterBucket,
  HintFilterValue,
  JoinBucket,
  PlanFilterMatchMode,
  ScanBucket,
  SortBucket,
  SubmitHistoryPlanFilters,
} from '../types/domain';

type BucketFilterValue = ScanBucket | JoinBucket | FilterBucket | SortBucket | AggregateBucket;
export type PlanBucketSectionKey = 'scanBucket' | 'joinBucket' | 'filterBucket' | 'sortBucket' | 'aggregateBucket';
export type PlanSectionKey = PlanBucketSectionKey | 'hint';
export type PlanFilterDbms = DbmsType | 'all';
export type PlanFilterFieldKey = keyof Pick<
  SubmitHistoryPlanFilters,
  'scanBuckets' | 'joinBuckets' | 'filterBuckets' | 'sortBuckets' | 'aggregateBuckets'
>;

interface BucketFilterDefinition {
  key: PlanBucketSectionKey;
  label: string;
  options: readonly BucketFilterValue[];
}

interface ExecutionPlanDetailGroup {
  sectionKey: PlanSectionKey;
  sectionLabel: string;
  labels: string[];
}

const HINT_INDEX = 30;

export const FILTER_MODE_OPTIONS: Array<{ key: PlanFilterMatchMode; label: string }> = [
  { key: 'and', label: 'AND' },
  { key: 'or', label: 'OR' },
];

export const HINT_FILTER_OPTIONS: readonly HintFilterValue[] = ['USED', 'UNUSED'];

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

const ALL_DBMS_FILTERS: BucketFilterDefinition[] = [
  { key: 'scanBucket', label: 'Scan', options: ['FULL_SCAN', 'ROWID_ACCESS', 'INDEX_SCAN', 'BITMAP_SCAN', 'TID_SCAN', 'DERIVED_SCAN', 'REMOTE_SCAN', 'OTHERS'] },
  { key: 'joinBucket', label: 'Join', options: ['NONE', 'NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'CARTESIAN_JOIN', 'OTHERS'] },
  { key: 'filterBucket', label: 'Filter', options: ['NONE', 'ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'OTHERS'] },
  { key: 'sortBucket', label: 'Sort', options: ['NONE', 'PLAIN_SORT', 'INCREMENTAL_SORT', 'ORDER_SORT', 'GROUP_SORT', 'UNIQUE_SORT', 'WINDOW_SORT', 'OTHERS'] },
  { key: 'aggregateBucket', label: 'Aggregate', options: ['NONE', 'PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'MIXED_AGG', 'WINDOW_AGG', 'UNIQUE_AGG', 'SET_AGG', 'OTHERS'] },
];

const BUCKET_PLAN_INDEXES_BY_DBMS: Record<DbmsType, Record<PlanBucketSectionKey, Partial<Record<BucketFilterValue, number[]>>>> = {
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

export function createEmptySubmitHistoryPlanFilters(): SubmitHistoryPlanFilters {
  return {
    matchMode: 'or',
    scanBuckets: [],
    joinBuckets: [],
    filterBuckets: [],
    sortBuckets: [],
    aggregateBuckets: [],
    hintFilters: [],
  };
}

export function buildAvailableBucketFilters(dbms: PlanFilterDbms): BucketFilterDefinition[] {
  return dbms === 'all' ? ALL_DBMS_FILTERS : BUCKET_FILTERS_BY_DBMS[dbms];
}

export function normalizePlanFilters(filters: SubmitHistoryPlanFilters, dbms: PlanFilterDbms): SubmitHistoryPlanFilters {
  const definitions = buildAvailableBucketFilters(dbms);
  const availableBySection = new Map(
    definitions.map((definition) => [definition.key, new Set(definition.options)]),
  );

  return {
    matchMode: filters.matchMode,
    scanBuckets: filters.scanBuckets.filter((value) => availableBySection.get('scanBucket')?.has(value) ?? false),
    joinBuckets: filters.joinBuckets.filter((value) => availableBySection.get('joinBucket')?.has(value) ?? false),
    filterBuckets: filters.filterBuckets.filter((value) => availableBySection.get('filterBucket')?.has(value) ?? false),
    sortBuckets: filters.sortBuckets.filter((value) => availableBySection.get('sortBucket')?.has(value) ?? false),
    aggregateBuckets: filters.aggregateBuckets.filter((value) => availableBySection.get('aggregateBucket')?.has(value) ?? false),
    hintFilters: filters.hintFilters.filter((value) => HINT_FILTER_OPTIONS.includes(value)),
  };
}

export function formatBucketDisplayLabel(value: BucketFilterValue) {
  if (value === 'NONE') {
    return '없음';
  }

  const normalizedSource = value.toLowerCase().endsWith('_agg') ? value.toLowerCase().replace(/_agg$/, '') : value.toLowerCase();
  const normalized = normalizedSource.replaceAll('_', ' ');
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

export function formatHintFilterLabel(value: HintFilterValue) {
  return value === 'USED' ? '사용' : '미사용';
}

export function summarizePlanFilters(dbms: PlanFilterDbms, filters: SubmitHistoryPlanFilters) {
  const definitions = buildAvailableBucketFilters(dbms);
  const pieces = definitions.flatMap((definition) => {
    const selectedValues = filters[toPlanFilterFieldKey(definition.key)];
    if (selectedValues.length === 0) {
      return [];
    }

    return [`${definition.label}: ${selectedValues.map(formatBucketDisplayLabel).join(', ')}`];
  });

  if (filters.hintFilters.length > 0) {
    pieces.push(`Hint: ${filters.hintFilters.map(formatHintFilterLabel).join(', ')}`);
  }

  if (pieces.length === 0) {
    return '실행계획 요소';
  }

  const preview = pieces.slice(0, 2).join(' · ');
  return `${filters.matchMode.toUpperCase()} · ${preview}${pieces.length > 2 ? ` 외 ${pieces.length - 2}` : ''}`;
}

export function getPlanElementButtonLabel(dbms: DbmsType, executionPlanElement: number) {
  const detailCount = getExecutionPlanDetailGroups(dbms, executionPlanElement)
    .flatMap((group) => group.labels)
    .length;

  return detailCount === 0 ? '요소 없음' : `요소 ${detailCount}개`;
}

export function getExecutionPlanDetailGroups(dbms: DbmsType, executionPlanElement: number): ExecutionPlanDetailGroup[] {
  const groups: ExecutionPlanDetailGroup[] = BUCKET_FILTERS_BY_DBMS[dbms].flatMap((definition) => {
    const labels = definition.options
      .filter((value) => value !== 'NONE' && value !== 'OTHERS')
      .filter((value) => matchesBucketFilter(executionPlanElement, dbms, definition.key, value))
      .map(formatBucketDisplayLabel);

    return labels.length > 0
      ? [{ sectionKey: definition.key, sectionLabel: definition.label, labels } satisfies ExecutionPlanDetailGroup]
      : [];
  });

  if (hasPlanElement(executionPlanElement, HINT_INDEX)) {
    groups.push({ sectionKey: 'hint', sectionLabel: 'Hint', labels: ['사용'] });
  }

  return groups;
}

export function toPlanFilterFieldKey(sectionKey: PlanBucketSectionKey): PlanFilterFieldKey {
  return `${sectionKey}s` as PlanFilterFieldKey;
}

function matchesBucketFilter(mask: number, dbms: DbmsType, filterKey: PlanBucketSectionKey, value: BucketFilterValue) {
  if (value === 'NONE') {
    return !hasAnyPlanElement(mask, getSectionKnownIndexes(dbms, filterKey));
  }

  const indexes = BUCKET_PLAN_INDEXES_BY_DBMS[dbms][filterKey][value] ?? [];
  return indexes.length > 0 && hasAnyPlanElement(mask, indexes);
}

function getSectionKnownIndexes(dbms: DbmsType, filterKey: PlanBucketSectionKey) {
  const filterIndexes = BUCKET_PLAN_INDEXES_BY_DBMS[dbms][filterKey];
  return [...new Set(Object.entries(filterIndexes)
    .filter(([value]) => value !== 'NONE' && value !== 'OTHERS')
    .flatMap(([, indexes]) => indexes ?? []))];
}

function hasAnyPlanElement(mask: number, indexes: number[]) {
  return indexes.some((index) => hasPlanElement(mask, index));
}

function hasPlanElement(mask: number, index: number) {
  return (mask & (1 << index)) !== 0;
}

export type { BucketFilterDefinition, BucketFilterValue, ExecutionPlanDetailGroup };
