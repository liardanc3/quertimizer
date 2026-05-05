import { getApiBaseUrl } from '@/shared/api/auth-api';
import { createApiErrorFromResponse, getUiTextValue } from '@/shared/config/ui-text';
import type { DbmsType, RankingEntry, RankingMetricKey } from '@/shared/api/domain';

interface RankMonthlyDeltaResponse {
  solvedCount?: number;
  avgExecutionPercentile?: number;
}

interface RankListItemResponse {
  handle?: string;
  solvedCount?: number;
  avgExecutionPercentile?: number;
  totalSubmitCount?: number;
  successSubmitCount?: number;
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
  pageSize: number;
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

const rankGetRequestPromises = new Map<string, Promise<unknown>>();

function toRankingEntry(rank: RankListItemResponse) {
  return {
    handle: rank.handle!,
    solvedCount: rank.solvedCount!,
    avgExecutionPercentile: rank.avgExecutionPercentile!,
    totalSubmitCount: rank.totalSubmitCount!,
    successSubmitCount: rank.successSubmitCount!,
    monthlyRankDelta: {
      solvedCount: rank.monthlyRankDelta?.solvedCount ?? 0,
      avgExecutionPercentile: rank.monthlyRankDelta?.avgExecutionPercentile ?? 0,
    },
  } satisfies RankingEntry;
}

function requestRankGet<T>(path: string, execute: () => Promise<T>): Promise<T> {
  const requestKey = `GET:${path}`;
  const inFlightRequest = rankGetRequestPromises.get(requestKey);
  if (inFlightRequest != null) {
    return inFlightRequest as Promise<T>;
  }

  const nextRequest = execute().finally(() => {
    rankGetRequestPromises.delete(requestKey);
  });

  rankGetRequestPromises.set(requestKey, nextRequest);
  return nextRequest;
}

export async function fetchRanks(params: FetchRanksParams): Promise<RankPage> {
  const searchParams = new URLSearchParams({
    page: String(params.page),
    pageSize: String(params.pageSize),
    dbms: params.dbms,
    sortKey: params.sortKey,
  });

  if (params.query.trim() !== '') {
    searchParams.set('query', params.query.trim());
  }

  const requestPath = `/ranks?${searchParams.toString()}`;

  return requestRankGet(requestPath, async () => {
    let response: Response;

    try {
      response = await fetch(`${getApiBaseUrl()}${requestPath}`, {
        method: 'GET',
        credentials: 'include',
      });
    } catch {
      throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
    }

    if (!response.ok) {
      throw await createApiErrorFromResponse(response, getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
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
            (rank): rank is Required<Pick<RankListItemResponse, 'handle' | 'solvedCount' | 'avgExecutionPercentile' | 'totalSubmitCount' | 'successSubmitCount'>> & RankListItemResponse =>
              typeof rank.handle === 'string' &&
              typeof rank.solvedCount === 'number' &&
              typeof rank.avgExecutionPercentile === 'number' &&
              typeof rank.totalSubmitCount === 'number' &&
              typeof rank.successSubmitCount === 'number',
          )
          .map(toRankingEntry),
      };
    } catch {
      throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
    }
  });
}
