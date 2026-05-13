export type DomainType = 'rdbms' | 'nosql';

export type Difficulty = '입문' | '중급' | '고급';

export type DbmsType = 'postgresql' | 'mysql';
export type ScanBucket =
  | 'FULL_SCAN'
  | 'FULL_TABLE_SCAN'
  | 'INDEX_SCAN'
  | 'BITMAP_SCAN'
  | 'TID_SCAN'
  | 'RANGE_SCAN'
  | 'REF_SCAN'
  | 'CONST_SCAN'
  | 'DERIVED_SCAN'
  | 'OTHERS';
export type JoinBucket = 'NONE' | 'NESTED_LOOP' | 'HASH_JOIN' | 'MERGE_JOIN' | 'JOIN_BUFFER' | 'OTHERS';
export type FilterBucket =
  | 'NONE'
  | 'ACCESS_FILTER'
  | 'POST_FILTER'
  | 'JOIN_FILTER'
  | 'INDEX_CONDITION'
  | 'ATTACHED_CONDITION'
  | 'FILTER_CONDITION'
  | 'OTHERS';
export type SortBucket =
  | 'NONE'
  | 'PLAIN_SORT'
  | 'INCREMENTAL_SORT'
  | 'FILESORT'
  | 'TEMPORARY_TABLE'
  | 'OTHERS';
export type AggregateBucket =
  | 'NONE'
  | 'PLAIN_AGG'
  | 'GROUP_AGG'
  | 'HASH_AGG'
  | 'MIXED_AGG'
  | 'WINDOW_AGG'
  | 'UNIQUE_AGG'
  | 'SET_AGG'
  | 'GROUPING_OPERATION'
  | 'WINDOW_OPERATION'
  | 'AGGREGATE'
  | 'OTHERS';

export type PlanFilterMatchMode = 'and' | 'or';
export type HintFilterValue = 'USED' | 'UNUSED';
export type SubmitHistoryJudge = 'all' | 'success' | 'fail';

export interface SubmitHistoryPlanFilters {
  matchMode: PlanFilterMatchMode;
  scanBuckets: ScanBucket[];
  joinBuckets: JoinBucket[];
  filterBuckets: FilterBucket[];
  sortBuckets: SortBucket[];
  aggregateBuckets: AggregateBucket[];
  hintFilters: HintFilterValue[];
}

export interface SubmitHistoryEntry {
  submitId: string;
  handle: string;
  dbms: DbmsType;
  problemId: string;
  submittedAt: string;
  success: boolean;
  message: string;
  submittedSql: string;
  cost: number;
  executionPlanElement: number;
}

export interface SubmitHistoryPageData {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  problemIds: string[];
  histories: SubmitHistoryEntry[];
}

export interface ProblemSubmittedHistory {
  dbms: DbmsType;
  handle: string;
  executionPlanElement: number;
  executionTimeMs: number;
  cost?: number;
}

export interface ProblemSummary {
  id: string;
  domain: DomainType;
  number: number;
  problemNumber?: string;
  title: string;
  preview: string;
  tags: string[];
  difficulty: Difficulty;
  solvedCount: number;
  totalSubmitCount?: number;
  successSubmitCount?: number;
  solvedAt?: string;
  isSolved?: boolean | null;
  submittedHistories?: ProblemSubmittedHistory[];
}

export interface ProblemDetail extends ProblemSummary {
  description: string;
  schemaInfo: string;
  inputExample: string;
  outputExample: string;
  starterSql: string;
  dbmsOptions: DbmsType[];
  disabledDbms: DbmsType[];
}

export interface RankingEntry {
  rank: number;
  handle: string;
  solvedCount: number;
  avgExecutionPercentile: number;
  totalSubmitCount: number;
  successSubmitCount: number;
  monthlyRankDelta: Record<RankingMetricKey, number>;
}

export type RankingMetricKey = 'solvedCount' | 'avgExecutionPercentile';

export type CommunityPostCategory = 'tip' | 'question' | 'discussion' | 'notice';

export interface CommunityPostSummary {
  id: string;
  title: string;
  authorHandle: string;
  excerpt: string;
  content: string;
  contentJson?: string;
  contentHtml?: string;
  tags: string[];
  category: CommunityPostCategory;
  createdAt: string;
  updatedAt?: string;
  views: number;
  likes: number;
  comments: number;
  likedByCurrentUser?: boolean;
  editable?: boolean;
  isPinned?: boolean;
  isResolved?: boolean;
}

export interface CommunityComment {
  id: string;
  authorHandle: string;
  content: string;
  createdAt: string;
  likes: number;
  likedByCurrentUser?: boolean;
  replies: CommunityComment[];
}
