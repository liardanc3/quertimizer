import { getApiBaseUrl } from './authApi';
import type { DbmsType, RankingEntry, RankingMetricKey } from '../types/domain';

interface RankMonthlyDeltaResponse {
  solvedCount?: number;
  avgExecutionPercentile?: number;
}

interface RankListItemResponse {
  handle?: string;
  solvedCount?: number;
  avgExecutionPercentile?: number;
  monthlyRankDelta?: RankMonthlyDeltaResponse;
}

interface RankPageResponse {
  currentPage?: number;
  pageSize?: number;
  totalCount?: number;
  totalPages?: number;
  ranks?: RankListItemResponse[];
}

export interface FetchRanksParams {
  page: number;
  dbms: DbmsType;
  query: string;
  sortKey: RankingMetricKey;
}

export interface RankPage {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  ranks: RankingEntry[];
}

function toRankingEntry(rank: RankListItemResponse) {
  return {
    handle: rank.handle!,
    solvedCount: rank.solvedCount!,
    avgExecutionPercentile: rank.avgExecutionPercentile!,
    monthlyRankDelta: {
      solvedCount: rank.monthlyRankDelta?.solvedCount ?? 0,
      avgExecutionPercentile: rank.monthlyRankDelta?.avgExecutionPercentile ?? 0,
    },
  } satisfies RankingEntry;
}

export async function fetchRanks(params: FetchRanksParams): Promise<RankPage> {
  let response: Response;

  const searchParams = new URLSearchParams({
    page: String(params.page),
    dbms: params.dbms,
    sortKey: params.sortKey,
  });

  if (params.query.trim() !== '') {
    searchParams.set('query', params.query.trim());
  }

  try {
    response = await fetch(`${getApiBaseUrl()}/ranks?${searchParams.toString()}`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('랭킹 조회에 실패했다.');
  }

  if (!response.ok) {
    throw new Error('랭킹 조회에 실패했다.');
  }

  try {
    const data = (await response.json()) as RankPageResponse;
    if (
      typeof data.currentPage !== 'number' ||
      typeof data.pageSize !== 'number' ||
      typeof data.totalCount !== 'number' ||
      typeof data.totalPages !== 'number' ||
      !Array.isArray(data.ranks)
    ) {
      throw new Error();
    }

    return {
      currentPage: data.currentPage,
      pageSize: data.pageSize,
      totalCount: data.totalCount,
      totalPages: data.totalPages,
      ranks: data.ranks
        .filter(
          (rank): rank is Required<Pick<RankListItemResponse, 'handle' | 'solvedCount' | 'avgExecutionPercentile'>> & RankListItemResponse =>
            typeof rank.handle === 'string' &&
            typeof rank.solvedCount === 'number' &&
            typeof rank.avgExecutionPercentile === 'number',
        )
        .map(toRankingEntry),
    };
  } catch {
    throw new Error('랭킹 조회에 실패했다.');
  }
}
