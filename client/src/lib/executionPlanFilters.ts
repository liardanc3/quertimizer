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
import { getUiText, getUiTextValue } from './uiText';

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

interface BucketFilterDefinitionSource {
  key: PlanBucketSectionKey;
  labelKey: string;
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

const BUCKET_FILTERS_BY_DBMS: Record<DbmsType, BucketFilterDefinitionSource[]> = {
  postgresql: [
    { key: 'scanBucket', labelKey: 'RUNTIME_SCAN_LABEL', options: ['FULL_SCAN', 'INDEX_SCAN', 'BITMAP_SCAN', 'TID_SCAN', 'DERIVED_SCAN', 'OTHERS'] },
    { key: 'joinBucket', labelKey: 'RUNTIME_JOIN_LABEL', options: ['NONE', 'NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'OTHERS'] },
    { key: 'filterBucket', labelKey: 'RUNTIME_FILTER_LABEL', options: ['NONE', 'ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'OTHERS'] },
    { key: 'sortBucket', labelKey: 'RUNTIME_SORT_LABEL', options: ['NONE', 'PLAIN_SORT', 'INCREMENTAL_SORT', 'OTHERS'] },
    { key: 'aggregateBucket', labelKey: 'RUNTIME_AGGREGATE_LABEL', options: ['NONE', 'PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'MIXED_AGG', 'WINDOW_AGG', 'UNIQUE_AGG', 'SET_AGG', 'OTHERS'] },
  ],
  mysql: [
    { key: 'scanBucket', labelKey: 'RUNTIME_SCAN_LABEL', options: ['FULL_TABLE_SCAN', 'INDEX_SCAN', 'RANGE_SCAN', 'REF_SCAN', 'CONST_SCAN', 'DERIVED_SCAN', 'OTHERS'] },
    { key: 'joinBucket', labelKey: 'RUNTIME_JOIN_LABEL', options: ['NONE', 'NESTED_LOOP', 'HASH_JOIN', 'JOIN_BUFFER', 'OTHERS'] },
    { key: 'filterBucket', labelKey: 'RUNTIME_FILTER_LABEL', options: ['NONE', 'INDEX_CONDITION', 'ATTACHED_CONDITION', 'FILTER_CONDITION', 'OTHERS'] },
    { key: 'sortBucket', labelKey: 'RUNTIME_SORT_LABEL', options: ['NONE', 'FILESORT', 'TEMPORARY_TABLE', 'OTHERS'] },
    { key: 'aggregateBucket', labelKey: 'RUNTIME_AGGREGATE_LABEL', options: ['NONE', 'GROUPING_OPERATION', 'WINDOW_OPERATION', 'AGGREGATE', 'OTHERS'] },
  ],
};

const ALL_DBMS_FILTERS: BucketFilterDefinitionSource[] = [
  { key: 'scanBucket', labelKey: 'RUNTIME_SCAN_LABEL', options: ['FULL_SCAN', 'FULL_TABLE_SCAN', 'INDEX_SCAN', 'BITMAP_SCAN', 'TID_SCAN', 'RANGE_SCAN', 'REF_SCAN', 'CONST_SCAN', 'DERIVED_SCAN', 'OTHERS'] },
  { key: 'joinBucket', labelKey: 'RUNTIME_JOIN_LABEL', options: ['NONE', 'NESTED_LOOP', 'MERGE_JOIN', 'HASH_JOIN', 'JOIN_BUFFER', 'OTHERS'] },
  { key: 'filterBucket', labelKey: 'RUNTIME_FILTER_LABEL', options: ['NONE', 'ACCESS_FILTER', 'POST_FILTER', 'JOIN_FILTER', 'INDEX_CONDITION', 'ATTACHED_CONDITION', 'FILTER_CONDITION', 'OTHERS'] },
  { key: 'sortBucket', labelKey: 'RUNTIME_SORT_LABEL', options: ['NONE', 'PLAIN_SORT', 'INCREMENTAL_SORT', 'FILESORT', 'TEMPORARY_TABLE', 'OTHERS'] },
  { key: 'aggregateBucket', labelKey: 'RUNTIME_AGGREGATE_LABEL', options: ['NONE', 'PLAIN_AGG', 'GROUP_AGG', 'HASH_AGG', 'MIXED_AGG', 'WINDOW_AGG', 'UNIQUE_AGG', 'SET_AGG', 'GROUPING_OPERATION', 'WINDOW_OPERATION', 'AGGREGATE', 'OTHERS'] },
];

const BUCKET_PLAN_INDEXES_BY_DBMS: Record<DbmsType, Record<PlanBucketSectionKey, Partial<Record<BucketFilterValue, number[]>>>> = {
  postgresql: {
    scanBucket: { FULL_SCAN: [0, 5], INDEX_SCAN: [1, 2], BITMAP_SCAN: [3, 4], TID_SCAN: [6], DERIVED_SCAN: [7, 8, 9, 10] },
    joinBucket: { NESTED_LOOP: [13], MERGE_JOIN: [12], HASH_JOIN: [11] },
    filterBucket: { ACCESS_FILTER: [29], POST_FILTER: [28] },
    sortBucket: { PLAIN_SORT: [16], INCREMENTAL_SORT: [17] },
    aggregateBucket: { GROUP_AGG: [15], HASH_AGG: [14], UNIQUE_AGG: [19] },
  },
  mysql: {
    scanBucket: { FULL_TABLE_SCAN: [0], INDEX_SCAN: [1], RANGE_SCAN: [2], REF_SCAN: [3, 4], CONST_SCAN: [5], DERIVED_SCAN: [7, 8] },
    joinBucket: { NESTED_LOOP: [10], HASH_JOIN: [11], JOIN_BUFFER: [23] },
    filterBucket: { FILTER_CONDITION: [14], INDEX_CONDITION: [15], ATTACHED_CONDITION: [16] },
    sortBucket: { FILESORT: [17], TEMPORARY_TABLE: [18] },
    aggregateBucket: { GROUPING_OPERATION: [19], WINDOW_OPERATION: [20], AGGREGATE: [21] },
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
  const definitions = dbms === 'all' ? ALL_DBMS_FILTERS : BUCKET_FILTERS_BY_DBMS[dbms];

  return definitions.map((definition) => ({
    key: definition.key,
    label: getBucketSectionLabel(definition.labelKey),
    options: definition.options,
  }));
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
  return getUiTextValue(bucketValueToLabelKey(value), getBucketDisplayFallback(value));
}

export function formatHintFilterLabel(value: HintFilterValue) {
  return value === 'USED'
    ? getUiTextValue('RUNTIME_USED_LABEL', '사용')
    : getUiTextValue('RUNTIME_UNUSED_LABEL', '미사용');
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
    pieces.push(`${getUiTextValue('COMMON_HINT_LABEL', 'Hint')}: ${filters.hintFilters.map(formatHintFilterLabel).join(', ')}`);
  }

  if (pieces.length === 0) {
    return getUiTextValue('RUNTIME_FILTER_TITLE', '실행 계획 요소');
  }

  const preview = pieces.slice(0, 2).join(' · ');
  const remainingPiece =
    pieces.length > 2
      ? ` ${getUiText('RUNTIME_FILTER_SUMMARY_EXTRA_LABEL', { count: pieces.length - 2 }, `외 ${pieces.length - 2}`)}`
      : '';
  return `${filters.matchMode.toUpperCase()} · ${preview}${remainingPiece}`;
}

export function getPlanElementButtonLabel(dbms: DbmsType, executionPlanElement: number) {
  const detailCount = getExecutionPlanDetailGroups(dbms, executionPlanElement)
    .flatMap((group) => group.labels)
    .length;

  return detailCount === 0
    ? getUiTextValue('RUNTIME_PLAN_ELEMENT_EMPTY_LABEL', '요소 없음')
    : getUiText('RUNTIME_PLAN_ELEMENT_COUNT_LABEL', { count: detailCount }, `요소 ${detailCount}개`);
}

export function getExecutionPlanDetailGroups(dbms: DbmsType, executionPlanElement: number): ExecutionPlanDetailGroup[] {
  const groups: ExecutionPlanDetailGroup[] = BUCKET_FILTERS_BY_DBMS[dbms].flatMap((definition) => {
    const labels = definition.options
      .filter((value) => value !== 'NONE' && value !== 'OTHERS')
      .filter((value) => matchesBucketFilter(executionPlanElement, dbms, definition.key, value))
      .map(formatBucketDisplayLabel);

    return labels.length > 0
      ? [{ sectionKey: definition.key, sectionLabel: getBucketSectionLabel(definition.labelKey), labels } satisfies ExecutionPlanDetailGroup]
      : [];
  });

  if (hasPlanElement(executionPlanElement, HINT_INDEX)) {
    groups.push({
      sectionKey: 'hint',
      sectionLabel: getUiTextValue('COMMON_HINT_LABEL', 'Hint'),
      labels: [getUiTextValue('RUNTIME_USED_LABEL', '사용')],
    });
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
    case 'FULL_TABLE_SCAN':
      return 'RUNTIME_FULL_TABLE_SCAN_LABEL';
    case 'INDEX_SCAN':
      return 'RUNTIME_INDEX_SCAN_LABEL';
    case 'BITMAP_SCAN':
      return 'RUNTIME_BITMAP_SCAN_LABEL';
    case 'TID_SCAN':
      return 'RUNTIME_TID_SCAN_LABEL';
    case 'RANGE_SCAN':
      return 'RUNTIME_RANGE_SCAN_LABEL';
    case 'REF_SCAN':
      return 'RUNTIME_REF_SCAN_LABEL';
    case 'CONST_SCAN':
      return 'RUNTIME_CONST_SCAN_LABEL';
    case 'DERIVED_SCAN':
      return 'RUNTIME_DERIVED_SCAN_LABEL';
    case 'NESTED_LOOP':
      return 'RUNTIME_NESTED_LOOP_LABEL';
    case 'MERGE_JOIN':
      return 'RUNTIME_MERGE_JOIN_LABEL';
    case 'HASH_JOIN':
      return 'RUNTIME_HASH_JOIN_LABEL';
    case 'JOIN_BUFFER':
      return 'RUNTIME_JOIN_BUFFER_LABEL';
    case 'ACCESS_FILTER':
      return 'RUNTIME_ACCESS_FILTER_LABEL';
    case 'POST_FILTER':
      return 'RUNTIME_POST_FILTER_LABEL';
    case 'JOIN_FILTER':
      return 'RUNTIME_JOIN_FILTER_LABEL';
    case 'INDEX_CONDITION':
      return 'RUNTIME_INDEX_CONDITION_LABEL';
    case 'ATTACHED_CONDITION':
      return 'RUNTIME_ATTACHED_CONDITION_LABEL';
    case 'FILTER_CONDITION':
      return 'RUNTIME_FILTER_CONDITION_LABEL';
    case 'PLAIN_SORT':
      return 'RUNTIME_PLAIN_SORT_LABEL';
    case 'INCREMENTAL_SORT':
      return 'RUNTIME_INCREMENTAL_SORT_LABEL';
    case 'FILESORT':
      return 'RUNTIME_FILESORT_LABEL';
    case 'TEMPORARY_TABLE':
      return 'RUNTIME_TEMPORARY_TABLE_LABEL';
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
    case 'GROUPING_OPERATION':
      return 'RUNTIME_GROUPING_OPERATION_LABEL';
    case 'WINDOW_OPERATION':
      return 'RUNTIME_WINDOW_OPERATION_LABEL';
    case 'AGGREGATE':
      return 'RUNTIME_AGGREGATE_OPERATION_LABEL';
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

export type { BucketFilterDefinition, BucketFilterValue, ExecutionPlanDetailGroup };
