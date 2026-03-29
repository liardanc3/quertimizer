import type { DbmsType, RankingEntry, RankingLeaderboardByDbms } from '../types/domain';

const tierCycle = [
  'Diamond 2',
  'Diamond 4',
  'Platinum 1',
  'Platinum 3',
  'Gold 1',
  'Gold 3',
  'Silver 1',
  'Silver 3',
];

function createDeltaMap(
  solvedCount: number,
  avgExecutionPercentile: number,
  avgScanRowsPercentile: number
): RankingEntry['monthlyRankDelta'] {
  return {
    solvedCount,
    avgExecutionPercentile,
    avgScanRowsPercentile,
  };
}

const seededRanking: RankingLeaderboardByDbms = {
  postgresql: [
    {
      handle: 'park-optimizer',
      name: '박옵티마이저',
      tier: 'Diamond 1',
      solvedCount: 142,
      avgExecutionPercentile: 6.3,
      avgScanRowsPercentile: 8.5,
      monthlyRankDelta: createDeltaMap(2, 1, 3),
    },
    {
      handle: 'lee-index',
      name: '이인덱스',
      tier: 'Platinum 4',
      solvedCount: 128,
      avgExecutionPercentile: 10.8,
      avgScanRowsPercentile: 12.1,
      monthlyRankDelta: createDeltaMap(-1, 2, 1),
    },
    {
      handle: 'han-hashjoin',
      name: '한해시조인',
      tier: 'Platinum 2',
      solvedCount: 116,
      avgExecutionPercentile: 13.7,
      avgScanRowsPercentile: 15.4,
      monthlyRankDelta: createDeltaMap(4, 3, -2),
    },
    {
      handle: 'kim-tuner',
      name: '김튜너',
      tier: 'Silver 2',
      solvedCount: 96,
      avgExecutionPercentile: 17.6,
      avgScanRowsPercentile: 19.2,
      monthlyRankDelta: createDeltaMap(6, 5, 4),
    },
    {
      handle: 'jung-zero-fullscan',
      name: '정제로풀스캔',
      tier: 'Gold 1',
      solvedCount: 88,
      avgExecutionPercentile: 19.4,
      avgScanRowsPercentile: 23.8,
      monthlyRankDelta: createDeltaMap(-3, -2, -4),
    },
    {
      handle: 'choi-join-master',
      name: '최조인장인',
      tier: 'Gold 3',
      solvedCount: 74,
      avgExecutionPercentile: 22.5,
      avgScanRowsPercentile: 25.9,
      monthlyRankDelta: createDeltaMap(1, -1, 2),
    },
  ],
  oracle: [
    {
      handle: 'park-optimizer',
      name: '박옵티마이저',
      tier: 'Diamond 1',
      solvedCount: 137,
      avgExecutionPercentile: 7.1,
      avgScanRowsPercentile: 9.2,
      monthlyRankDelta: createDeltaMap(1, 2, 1),
    },
    {
      handle: 'seo-plan-crafter',
      name: '서플랜크래프터',
      tier: 'Platinum 1',
      solvedCount: 118,
      avgExecutionPercentile: 11.5,
      avgScanRowsPercentile: 10.4,
      monthlyRankDelta: createDeltaMap(3, 4, 6),
    },
    {
      handle: 'kim-tuner',
      name: '김튜너',
      tier: 'Silver 2',
      solvedCount: 102,
      avgExecutionPercentile: 15.9,
      avgScanRowsPercentile: 14.6,
      monthlyRankDelta: createDeltaMap(5, 2, 5),
    },
    {
      handle: 'min-sort-tuner',
      name: '민소트튜너',
      tier: 'Gold 2',
      solvedCount: 97,
      avgExecutionPercentile: 16.8,
      avgScanRowsPercentile: 18.7,
      monthlyRankDelta: createDeltaMap(-2, -1, -3),
    },
    {
      handle: 'lee-index',
      name: '이인덱스',
      tier: 'Platinum 4',
      solvedCount: 93,
      avgExecutionPercentile: 18.3,
      avgScanRowsPercentile: 16.2,
      monthlyRankDelta: createDeltaMap(-1, 3, 2),
    },
    {
      handle: 'oh-cost-cutter',
      name: '오코스트커터',
      tier: 'Gold 4',
      solvedCount: 81,
      avgExecutionPercentile: 21.2,
      avgScanRowsPercentile: 20.5,
      monthlyRankDelta: createDeltaMap(2, -2, 1),
    },
  ],
};

function roundToOne(value: number) {
  return Number(value.toFixed(1));
}

function createGeneratedRanking(dbms: DbmsType, count: number): RankingEntry[] {
  const dbmsPrefix = dbms === 'postgresql' ? 'pg' : 'ora';
  const displayPrefix = dbms === 'postgresql' ? 'PG User' : 'Oracle User';
  const solvedBase = dbms === 'postgresql' ? 94 : 99;
  const executionBase = dbms === 'postgresql' ? 23.1 : 22.4;
  const scanBase = dbms === 'postgresql' ? 24.4 : 23.6;

  return Array.from({ length: count }, (_, index) => {
    const order = index + 1;
    const padded = String(order).padStart(3, '0');
    const solvedDrop = Math.floor(index / 3) + (index % 9);
    const executionOffset = index * 0.19 + (index % 5) * 0.17;
    const scanOffset = index * 0.21 + (index % 7) * 0.14;

    return {
      handle: `${dbmsPrefix}-user-${padded}`,
      name: `${displayPrefix} ${padded}`,
      tier: tierCycle[index % tierCycle.length],
      solvedCount: Math.max(6, solvedBase - solvedDrop),
      avgExecutionPercentile: roundToOne(Math.min(98.9, executionBase + executionOffset)),
      avgScanRowsPercentile: roundToOne(Math.min(99.1, scanBase + scanOffset)),
      monthlyRankDelta: createDeltaMap(
        ((index * 5) % 19) - 9,
        ((index * 7) % 17) - 8,
        ((index * 11) % 21) - 10
      ),
    };
  });
}

export const mockRanking: RankingLeaderboardByDbms = {
  postgresql: [...seededRanking.postgresql, ...createGeneratedRanking('postgresql', 300)],
  oracle: [...seededRanking.oracle, ...createGeneratedRanking('oracle', 300)],
};
