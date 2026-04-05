export type DomainType = 'rdbms' | 'nosql';

export type HomeSectionType = 'ranking' | 'problems';

export type ProblemViewMode = 'tagged' | 'spoilerFree';

export type Difficulty = '입문' | '중급' | '고급';

export type DbmsType = 'postgresql' | 'oracle';
export type ScanBucket =
  | 'FULL_SCAN'
  | 'ROWID_ACCESS'
  | 'INDEX_SCAN'
  | 'BITMAP_SCAN'
  | 'TID_SCAN'
  | 'DERIVED_SCAN'
  | 'REMOTE_SCAN'
  | 'OTHERS';
export type JoinBucket = 'NONE' | 'NESTED_LOOP' | 'HASH_JOIN' | 'MERGE_JOIN' | 'CARTESIAN_JOIN' | 'OTHERS';
export type FilterBucket =
  | 'NONE'
  | 'ACCESS_FILTER'
  | 'POST_FILTER'
  | 'JOIN_FILTER'
  | 'OTHERS';
export type SortBucket =
  | 'NONE'
  | 'PLAIN_SORT'
  | 'INCREMENTAL_SORT'
  | 'ORDER_SORT'
  | 'GROUP_SORT'
  | 'UNIQUE_SORT'
  | 'WINDOW_SORT'
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
  | 'OTHERS';

export type JudgeStatus = 'success' | 'fail';

export interface ResultRow {
  columns: string[];
}

export interface MockResult {
  status: JudgeStatus;
  message: string;
  executionTimeMs: number;
  scanRows: number;
  cost: number;
  indexUsed: boolean;
  fullScan: boolean;
  rows: ResultRow[];
}

export interface RuntimeBucket {
  startMs: number;
  count: number;
}

export interface RuntimeLeaderboardEntry {
  nickname: string;
  timeMs: number;
  submittedAt: string;
}

export interface ProblemSubmittedHistory {
  dbms: DbmsType;
  userId: string;
  executionPlanElement: number;
  executionTimeMs: number;
}

export interface RuntimeSample {
  nickname: string;
  timeMs: number;
  rowsScanned: number;
  submittedAt: string;
  indexUsed: boolean;
  fullScan: boolean;
  hintUsed: boolean;
  scanBucket: ScanBucket;
  joinBucket: JoinBucket;
  filterBucket: FilterBucket;
  sortBucket: SortBucket;
  aggregateBucket: AggregateBucket;
  isMine?: boolean;
}

export interface RuntimeTuningStats {
  p90TimeMs: number;
  indexUsageRate: number;
  fullScanRate: number;
  averageRowsScanned: number;
  bufferHitRate: number;
  tempSpillRate: number;
  hintUsageRate: number;
}

export interface RuntimeDistribution {
  bucketSizeMs: number;
  buckets: RuntimeBucket[];
  fastestTimeMs: number;
  fastestNickname: string;
  fastestSolvedAt: string;
  averageTimeMs: number;
  medianTimeMs: number;
  standardDeviationMs: number;
  varianceMs: number;
  myTimeMs?: number;
  submissionCount: number;
  topPerformers: RuntimeLeaderboardEntry[];
  samples: RuntimeSample[];
  tuningStats: RuntimeTuningStats;
}

export type RuntimeDistributionByDbms = Partial<Record<DbmsType, RuntimeDistribution>>;

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
  solvedAt?: string;
  isSolved?: boolean | null;
  submittedHistories?: ProblemSubmittedHistory[];
  runtimeDistribution?: RuntimeDistribution;
  runtimeDistributions?: RuntimeDistributionByDbms;
}

export interface ProblemDetail extends ProblemSummary {
  description: string;
  schemaInfo: string;
  inputExample: string;
  outputExample: string;
  starterSql: string;
  dbmsOptions: DbmsType[];
  disabledDbms: DbmsType[];
  mockResult: MockResult;
}

export interface DbmsOption {
  id: DbmsType;
  label: string;
  disabled?: boolean;
}

export interface ProfileSolvedRecord {
  id: string;
  problemId: string;
  problemNumber: number;
  problemTitle: string;
  executionTimeMs: number;
  scanRows: number;
  solvedAt: string;
}

export interface ProfileLinks {
  blog?: string;
  github?: string;
  email?: string;
}

export type SqlEditorPreset = 'focused' | 'balanced' | 'analysis';
export type SqlVisibility = 'public' | 'followers' | 'private';

export interface ProfileSettings {
  defaultDbms: DbmsType;
  sqlEditorPreset: SqlEditorPreset;
  sqlVisibility: SqlVisibility;
}

export interface Profile {
  handle: string;
  name: string;
  tier: string;
  avatarUrl?: string;
  bio: string;
  links: ProfileLinks;
  solvedProblems: ProfileSolvedRecord[];
  settings: ProfileSettings;
  solvedCount: number;
}

export interface RankingEntry {
  handle: string;
  name: string;
  tier: string;
  solvedCount: number;
  avgExecutionPercentile: number;
  avgScanRowsPercentile: number;
  monthlyRankDelta: Record<RankingMetricKey, number>;
}

export type RankingLeaderboardByDbms = Record<DbmsType, RankingEntry[]>;
export type RankingMetricKey = 'solvedCount' | 'avgExecutionPercentile' | 'avgScanRowsPercentile';

export type CommunityPostCategory = 'tip' | 'question' | 'discussion' | 'notice';
export type CommunityTagKind = 'problem' | 'tech' | 'topic';

export interface CommunityTagDefinition {
  id: string;
  label: string;
  kind: CommunityTagKind;
  aliases: string[];
  usageCount: number;
  description: string;
}

export interface CommunityPostSummary {
  id: string;
  title: string;
  authorHandle: string;
  excerpt: string;
  content: string;
  contentHtml?: string;
  tags: string[];
  category: CommunityPostCategory;
  createdAt: string;
  updatedAt?: string;
  views: number;
  likes: number;
  comments: number;
  isPinned?: boolean;
  isResolved?: boolean;
}

export interface CommunityComment {
  id: string;
  authorHandle: string;
  content: string;
  createdAt: string;
  likes: number;
  replies: CommunityComment[];
}
