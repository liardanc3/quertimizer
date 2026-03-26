export type DomainType = 'rdbms' | 'nosql';

export type HomeSectionType = 'ranking' | 'problems';

export type ProblemViewMode = 'tagged' | 'spoilerFree';

export type Difficulty = '입문' | '중급' | '고급';

export type DbmsType = 'postgresql' | 'oracle';

export type JudgeStatus = 'success' | 'fail';

export interface ResultRow {
  columns: string[];
}

export interface MockResult {
  status: JudgeStatus;
  message: string;
  executionTimeMs: number;
  cost: number;
  indexUsed: boolean;
  fullScan: boolean;
  rows: ResultRow[];
}

export interface ProblemSummary {
  id: string;
  number: number;
  title: string;
  preview: string;
  tags: string[];
  difficulty: Difficulty;
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

export interface Profile {
  name: string;
  tier: string;
  solvedCount: number;
}
