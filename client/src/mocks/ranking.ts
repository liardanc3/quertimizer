import type { RankingEntry } from '../types/domain';

export const mockRanking: RankingEntry[] = [
  { rank: 1, name: '박옵티마이저', tier: 'Diamond 1', score: 9820, solvedCount: 213, avgExecutionTimeMs: 11.8 },
  { rank: 2, name: '김튜너', tier: 'Silver 2', score: 8510, solvedCount: 47, avgExecutionTimeMs: 18.4 },
  { rank: 3, name: '이인덱스', tier: 'Platinum 4', score: 8240, solvedCount: 121, avgExecutionTimeMs: 14.9 },
  { rank: 4, name: '정풀스캔제로', tier: 'Gold 1', score: 8015, solvedCount: 98, avgExecutionTimeMs: 16.1 },
  { rank: 5, name: '최조인장인', tier: 'Gold 3', score: 7760, solvedCount: 84, avgExecutionTimeMs: 17.3 },
];
