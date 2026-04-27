import { getApiBaseUrl } from './authApi';
import { createApiErrorFromResponse, getUiTextValue } from './uiText';
import type {
  DbmsType,
  SubmitHistoryEntry,
  SubmitHistoryJudge,
  SubmitHistoryPageData,
  SubmitHistoryPlanFilters,
} from '../types/domain';

interface SubmitHistoryItemResponse {
  submitId?: string;
  handle?: string;
  dbms?: string;
  problemId?: string;
  submittedAt?: string;
  success?: boolean;
  message?: string;
  submittedSql?: string;
  cost?: number;
  executionPlanElement?: number;
}

interface SubmitHistoryPageResponse {
  currentPage?: number;
  pageSize?: number;
  totalCount?: number;
  totalPages?: number;
  problemIds?: string[];
  histories?: SubmitHistoryItemResponse[];
}

type SubmitHistoryPlanFiltersByDbms = Record<DbmsType, SubmitHistoryPlanFilters>;

export interface FetchSubmitHistoriesParams {
  page: number;
  submitId: string;
  query: string;
  dbms: DbmsType | 'all';
  problemId: string;
  judge: SubmitHistoryJudge;
  costSort: 'none' | 'asc' | 'desc';
  planFiltersByDbms: SubmitHistoryPlanFiltersByDbms;
}

const submitHistoryGetRequestPromises = new Map<string, Promise<unknown>>();

function toDbmsType(value?: string) {
  return value === 'oracle' ? 'oracle' : 'postgresql';
}

function toSubmitHistoryEntry(item: Required<SubmitHistoryItemResponse>): SubmitHistoryEntry {
  return {
    submitId: item.submitId,
    handle: item.handle,
    dbms: toDbmsType(item.dbms),
    problemId: item.problemId,
    submittedAt: item.submittedAt,
    success: item.success,
    message: item.message,
    submittedSql: item.submittedSql,
    cost: item.cost,
    executionPlanElement: item.executionPlanElement,
  };
}

function appendCsv(searchParams: URLSearchParams, key: string, values: string[]) {
  if (values.length > 0) {
    searchParams.set(key, values.join(','));
  }
}

function requestSubmitHistoryGet<T>(path: string, execute: () => Promise<T>): Promise<T> {
  const requestKey = `GET:${path}`;
  const inFlightRequest = submitHistoryGetRequestPromises.get(requestKey);
  if (inFlightRequest != null) {
    return inFlightRequest as Promise<T>;
  }

  const nextRequest = execute().finally(() => {
    submitHistoryGetRequestPromises.delete(requestKey);
  });

  submitHistoryGetRequestPromises.set(requestKey, nextRequest);
  return nextRequest;
}

export async function fetchSubmitHistories(params: FetchSubmitHistoriesParams): Promise<SubmitHistoryPageData> {
  const searchParams = new URLSearchParams({
    page: String(params.page),
  });

  if (params.submitId.trim() !== '') {
    searchParams.set('submitId', params.submitId.trim());
  }

  if (params.query.trim() !== '') {
    searchParams.set('query', params.query.trim());
  }

  if (params.dbms !== 'all') {
    searchParams.set('dbms', params.dbms);
  }

  if (params.problemId.trim() !== '') {
    searchParams.set('problemId', params.problemId.trim());
  }

  if (params.judge !== 'all') {
    searchParams.set('judge', params.judge);
  }

  if (params.costSort !== 'none') {
    searchParams.set('costSort', params.costSort);
  }

  const { planFiltersByDbms } = params;
  const hasPlanFilters = Object.values(planFiltersByDbms).some((planFilters) =>
    planFilters.scanBuckets.length > 0 ||
    planFilters.joinBuckets.length > 0 ||
    planFilters.filterBuckets.length > 0 ||
    planFilters.sortBuckets.length > 0 ||
    planFilters.aggregateBuckets.length > 0 ||
    planFilters.hintFilters.length > 0,
  );

  if (hasPlanFilters) {
    searchParams.set('planMatchMode', planFiltersByDbms.postgresql.matchMode);

    (['postgresql', 'oracle'] as const).forEach((dbmsKey) => {
      const planFilters = planFiltersByDbms[dbmsKey];
      appendCsv(searchParams, `${dbmsKey}ScanBuckets`, planFilters.scanBuckets);
      appendCsv(searchParams, `${dbmsKey}JoinBuckets`, planFilters.joinBuckets);
      appendCsv(searchParams, `${dbmsKey}FilterBuckets`, planFilters.filterBuckets);
      appendCsv(searchParams, `${dbmsKey}SortBuckets`, planFilters.sortBuckets);
      appendCsv(searchParams, `${dbmsKey}AggregateBuckets`, planFilters.aggregateBuckets);
      appendCsv(searchParams, `${dbmsKey}HintFilters`, planFilters.hintFilters);
    });
  }

  const requestPath = `/submit-histories?${searchParams.toString()}`;

  return requestSubmitHistoryGet(requestPath, async () => {
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
      const data = (await response.json()) as SubmitHistoryPageResponse;
      if (
        typeof data.currentPage !== 'number' ||
        typeof data.pageSize !== 'number' ||
        typeof data.totalCount !== 'number' ||
        typeof data.totalPages !== 'number' ||
        !Array.isArray(data.problemIds) ||
        !Array.isArray(data.histories)
      ) {
        throw new Error();
      }

      return {
        currentPage: data.currentPage,
        pageSize: data.pageSize,
        totalCount: data.totalCount,
        totalPages: data.totalPages,
        problemIds: data.problemIds.filter((problemId): problemId is string => typeof problemId === 'string'),
        histories: data.histories
          .filter(
            (history): history is Required<SubmitHistoryItemResponse> =>
              typeof history.submitId === 'string' &&
              typeof history.handle === 'string' &&
              typeof history.problemId === 'string' &&
              typeof history.submittedAt === 'string' &&
              typeof history.success === 'boolean' &&
              typeof history.message === 'string' &&
              typeof history.submittedSql === 'string' &&
              typeof history.cost === 'number' &&
              typeof history.executionPlanElement === 'number',
          )
          .map(toSubmitHistoryEntry),
      };
    } catch {
      throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
    }
  });
}
