import { createPortal } from 'react-dom';
import { useEffect, useMemo, useRef, useState } from 'react';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import { DataTable } from '../components/common/DataTable';
import { LoadingOverlay } from '../components/common/LoadingSpinner';
import PageErrorState from '../components/common/PageErrorState';
import Pagination from '../components/common/Pagination';
import { PageToolbar, SearchForm, SegmentedTabs } from '../components/common/PageToolbar';
import SortIcon from '../components/icons/SortIcon';
import { replaceQueryState, useLocationSearch } from '../hooks/useLocationState';
import useDismissableLayer from '../hooks/useDismissableLayer';
import useRequestState from '../hooks/useRequestState';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '../lib/favoriteTabs';
import { getProfilePath, RANKING_PATH, navigate } from '../lib/navigation';
import { fetchRanks, type RankPage } from '../lib/rankApi';
import { formatPercent } from '../lib/formatters';
import { useUiText } from '../lib/uiText';
import type { DbmsType, RankingMetricKey } from '../types/domain';
import './HomePage.css';
import './SubmitHistoryPage.css';
import './RankingPage.css';

const PAGE_SIZE = 10;
const rankingLoadingRows = Array.from({ length: 10 }, (_, index) => index);

const dbmsOptions: DbmsType[] = ['postgresql', 'mysql'];

function readRankingDbmsFromSearch(search: string) {
  const dbms = new URLSearchParams(search).get('dbms');
  return dbms === 'mysql' ? 'mysql' : 'postgresql';
}

function buildRankingPath(dbms: DbmsType) {
  if (dbms === 'postgresql') {
    return RANKING_PATH;
  }

  return `${RANKING_PATH}?dbms=${encodeURIComponent(dbms)}`;
}

type LinkMenuState = {
  label: string;
  path: string;
  ariaLabel: string;
  left: number;
  top: number;
} | null;

interface RankingPageFavoriteSnapshot {
  selectedDbms: DbmsType;
  sortKey: RankingMetricKey;
  draftQuery: string;
  submittedQuery: string;
  requestedPage: number;
}

function createEmptyRankPage(): RankPage {
  return {
    currentPage: 1,
    pageSize: PAGE_SIZE,
    totalCount: 0,
    totalPages: 1,
    ranks: [],
  };
}

export default function RankingPage() {
  const { text } = useUiText();
  const locationSearch = useLocationSearch();
  const favoriteRestoreSnapshot = useMemo(() => readFavoriteRestoreSnapshot<RankingPageFavoriteSnapshot>('ranking'), []);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>(() => favoriteRestoreSnapshot?.selectedDbms ?? readRankingDbmsFromSearch(window.location.search));
  const [sortKey, setSortKey] = useState<RankingMetricKey>(() => favoriteRestoreSnapshot?.sortKey ?? 'solvedCount');
  const [draftQuery, setDraftQuery] = useState(() => favoriteRestoreSnapshot?.draftQuery ?? '');
  const [submittedQuery, setSubmittedQuery] = useState(() => favoriteRestoreSnapshot?.submittedQuery ?? '');
  const [requestedPage, setRequestedPage] = useState(() => favoriteRestoreSnapshot?.requestedPage ?? 1);
  const {
    data: rankPage,
    setData: setRankPage,
    isLoading,
    setIsLoading,
    error: loadError,
    beginRequest,
    failRequest,
  } = useRequestState<RankPage>(createEmptyRankPage);
  const [linkMenuState, setLinkMenuState] = useState<LinkMenuState>(null);
  const linkMenuRef = useRef<HTMLButtonElement | null>(null);
  const linkMenuLayerRefs = useMemo(() => [linkMenuRef], []);

  useEffect(() => {
    clearFavoriteRestoreSnapshot('ranking');
  }, []);

  const rankedEntries = useMemo(
    () =>
      rankPage.ranks.map((entry, index) => ({
        ...entry,
        rank: (rankPage.currentPage - 1) * rankPage.pageSize + index + 1,
      })),
    [rankPage],
  );

  useEffect(() => {
    const nextDbms = readRankingDbmsFromSearch(locationSearch);

    setSelectedDbms((currentDbms) => (currentDbms === nextDbms ? currentDbms : nextDbms));
  }, [locationSearch]);

  useEffect(() => {
    const nextPath = buildRankingPath(selectedDbms);
    replaceQueryState(nextPath);
  }, [selectedDbms]);

  useEffect(() => {
    let cancelled = false;

    async function loadRanks() {
      beginRequest();

      try {
        const fetchedRankPage = await fetchRanks({
          page: requestedPage,
          pageSize: PAGE_SIZE,
          dbms: selectedDbms,
          query: submittedQuery,
          sortKey,
        });

        if (cancelled) {
          return;
        }

        setRankPage(fetchedRankPage);
        if (fetchedRankPage.currentPage !== requestedPage) {
          setRequestedPage(fetchedRankPage.currentPage);
        }
      } catch (error) {
        if (cancelled) {
          return;
        }

        failRequest(error, text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadRanks();

    return () => {
      cancelled = true;
    };
  }, [requestedPage, selectedDbms, sortKey, submittedQuery]);

  useDismissableLayer({
    enabled: linkMenuState != null,
    refs: linkMenuLayerRefs,
    onDismiss: () => setLinkMenuState(null),
    dismissOnResize: true,
    dismissOnScroll: true,
  });

  function applySearch() {
    const willReload = draftQuery.trim() !== submittedQuery.trim() || requestedPage !== 1;
    if (!willReload) {
      return;
    }

    setIsLoading(true);
    setSubmittedQuery(draftQuery);
    setRequestedPage(1);
  }

  function requestRankPage(nextPage: number) {
    const normalizedPage = Math.min(rankPage.totalPages, Math.max(1, nextPage));
    if (normalizedPage === rankPage.currentPage) {
      return;
    }

    setIsLoading(true);
    setRequestedPage(normalizedPage);
  }

  function openHandleMenu(handle: string, button: HTMLButtonElement) {
    const rect = button.getBoundingClientRect();
    const menuWidth = 224;
    const viewportWidth = document.documentElement.clientWidth;
    const maxLeft = viewportWidth - menuWidth - 12;
    const nextLeft = Math.max(12, Math.min(rect.left, maxLeft));

    setLinkMenuState({
      label: text('RANKING_PROFILE_MOVE_LABEL', { handle }, '{handle} 프로필로 이동'),
      path: getProfilePath(handle),
      ariaLabel: text('RANKING_PROFILE_MOVE_MENU_LABEL', { handle }, '{handle} 프로필 이동'),
      left: nextLeft,
      top: rect.bottom + 8,
    });
  }

  const linkMenuContent =
    linkMenuState == null || typeof document === 'undefined'
      ? null
      : createPortal(
          <button
            ref={linkMenuRef}
            type="button"
            className="submit-history-link-menu"
            role="menuitem"
            aria-label={linkMenuState.ariaLabel}
            style={{ top: `${linkMenuState.top}px`, left: `${linkMenuState.left}px` }}
            onClick={() => {
              navigate(linkMenuState.path);
              setLinkMenuState(null);
            }}
          >
            <span className="submit-history-link-menu-label">{linkMenuState.label}</span>
          </button>,
          document.body,
        );

  return (
    <div className="page page-stack ranking-page submit-history-page home-page">
      <section className="panel-card compact problem-toolbar-card submit-history-toolbar-card ranking-toolbar-card">
        <PageToolbar className="problem-toolbar home-problem-toolbar-stack submit-history-toolbar-stack ranking-toolbar-stack">
          <SegmentedTabs
            className="solve-dbms-tab-row ranking-dbms-tab-row"
            label={text('RANKING_DBMS_TABLIST_LABEL', '랭킹 DBMS 선택')}
            tabs={dbmsOptions.map((dbmsOption) => ({
              value: dbmsOption,
              label: dbmsOption === 'mysql' ? text('COMMON_MYSQL_LABEL', 'MySQL') : text('COMMON_POSTGRESQL_LABEL', 'PostgreSQL'),
            }))}
            selectedValue={selectedDbms}
            onSelect={(dbmsOption) => {
              setIsLoading(true);
              setSelectedDbms(dbmsOption);
              setRequestedPage(1);
            }}
            actions={
            <FavoriteTabButton
              className="favorite-tab-toggle-end"
              label={text(
                'RANKING_FAVORITE_LABEL',
                { dbms: selectedDbms === 'mysql' ? text('COMMON_MYSQL_LABEL', 'MySQL') : text('COMMON_POSTGRESQL_LABEL', 'PostgreSQL') },
                '랭킹 / {dbms}',
              )}
              path={buildRankingPath(selectedDbms)}
              snapshot={{
                kind: 'ranking',
                payload: {
                  selectedDbms,
                  sortKey,
                  draftQuery,
                  submittedQuery,
                  requestedPage,
                },
              }}
            />
            }
          />

          <SearchForm
            className="problem-search-form home-problem-search-form ranking-search-form"
            fieldClassName="problem-search-field home-problem-search-field ranking-search-field"
            inputClassName="problem-search-input home-problem-search-input ranking-search-input"
            buttonClassName="problem-search-button home-problem-search-button"
            value={draftQuery}
            onChange={setDraftQuery}
            onSubmit={applySearch}
            placeholder={text('RANKING_HANDLE_SEARCH_PLACEHOLDER', 'Handle 검색')}
            label={text('RANKING_HANDLE_SEARCH_LABEL', 'Handle 검색')}
            submitLabel={text('RANKING_HANDLE_SEARCH_SUBMIT_LABEL', 'Handle 검색 실행')}
            buttonLabel={text('COMMON_SEARCH_BUTTON', '검색')}
          />
        </PageToolbar>
      </section>

      <section className="panel-card problem-board submit-history-board ranking-board data-board">
        {loadError.failed ? (
          <PageErrorState status={loadError.status} message={loadError.message} />
        ) : (
          <DataTable className="submit-history-table ranking-table" isLoading={isLoading} label={text('RANKING_TABLE_LABEL', '랭킹 목록')}>
            <div className="submit-history-row submit-history-head ranking-head" role="row">
              <div role="columnheader" className="submit-history-head-cell">{text('RANKING_RANK_COLUMN_LABEL', '순위')}</div>
              <div role="columnheader" className="submit-history-head-cell ranking-head-handle-cell">{text('COMMON_HANDLE_LABEL', 'Handle')}</div>
              <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                <span>{text('RANKING_SOLVED_COLUMN_LABEL', '해결한 문제')}</span>
                <button
                  type="button"
                  className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${sortKey === 'solvedCount' ? 'is-active' : ''}`}
                  aria-label={text('RANKING_SOLVED_SORT_DESC_LABEL', '해결한 문제 내림차순 정렬')}
                  onClick={() => {
                    if (sortKey !== 'solvedCount') {
                      setIsLoading(true);
                      setSortKey('solvedCount');
                      setRequestedPage(1);
                    }
                  }}
                >
                  <SortIcon direction={sortKey === 'solvedCount' ? 'desc' : 'none'} />
                </button>
              </div>
              <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                <span>{text('RANKING_COST_PERCENTILE_COLUMN_LABEL', '평균 Cost 백분위')}</span>
                <button
                  type="button"
                  className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${sortKey === 'avgExecutionPercentile' ? 'is-active' : ''}`}
                  aria-label={text('RANKING_COST_PERCENTILE_SORT_ASC_LABEL', '평균 Cost 백분위 오름차순 정렬')}
                  onClick={() => {
                    if (sortKey !== 'avgExecutionPercentile') {
                      setIsLoading(true);
                      setSortKey('avgExecutionPercentile');
                      setRequestedPage(1);
                    }
                  }}
                >
                  <SortIcon direction={sortKey === 'avgExecutionPercentile' ? 'asc' : 'none'} />
                </button>
              </div>
              <div role="columnheader" className="submit-history-head-cell">{text('RANKING_TOTAL_SUBMIT_COLUMN_LABEL', '전체 제출 수')}</div>
              <div role="columnheader" className="submit-history-head-cell">{text('RANKING_SUCCESS_SUBMIT_COLUMN_LABEL', '정답 제출 수')}</div>
            </div>

            {isLoading && rankedEntries.length === 0 ? (
              rankingLoadingRows.map((rowIndex) => (
                <div key={`ranking-loading-${rowIndex}`} className="submit-history-row submit-history-body ranking-body" role="row" aria-hidden="true">
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-mini" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                </div>
              ))
            ) : rankedEntries.length === 0 ? (
              <div className="submit-history-row submit-history-empty-row" role="row">
                <span className="submit-history-empty-cell" role="cell">
                  {text('RANKING_EMPTY_STATE', '조건에 맞는 랭킹이 없습니다.')}
                </span>
              </div>
            ) : (
              rankedEntries.map((entry) => (
                <article key={`${selectedDbms}-${entry.handle}`} className="submit-history-row submit-history-body ranking-body" role="row">
                  <span className="submit-history-cell" role="cell" data-label={text('RANKING_RANK_COLUMN_LABEL', '순위')}>
                    {entry.rank}
                  </span>
                  <span className="submit-history-cell" role="cell" data-label={text('COMMON_HANDLE_LABEL', 'Handle')}>
                    <button
                      type="button"
                      className="submit-history-link-button"
                      onClick={(event) => openHandleMenu(entry.handle, event.currentTarget)}
                      aria-label={text('RANKING_HANDLE_MENU_OPEN_LABEL', { handle: entry.handle }, '{handle} Handle 메뉴 열기')}
                    >
                      {entry.handle}
                    </button>
                  </span>
                  <span className="submit-history-cell ranking-emphasis-cell" role="cell" data-label={text('RANKING_SOLVED_COLUMN_LABEL', '해결한 문제')}>
                    {entry.solvedCount}
                  </span>
                  <span className="submit-history-cell ranking-emphasis-cell" role="cell" data-label={text('RANKING_COST_PERCENTILE_COLUMN_LABEL', '평균 Cost 백분위')}>
                    {formatPercent(entry.avgExecutionPercentile)}
                  </span>
                  <span className="submit-history-cell" role="cell" data-label={text('RANKING_TOTAL_SUBMIT_COLUMN_LABEL', '전체 제출 수')}>
                    {entry.totalSubmitCount}
                  </span>
                  <span className="submit-history-cell" role="cell" data-label={text('RANKING_SUCCESS_SUBMIT_COLUMN_LABEL', '정답 제출 수')}>
                    {entry.successSubmitCount}
                  </span>
                </article>
              ))
            )}

            {isLoading ? <LoadingOverlay className="ranking-table-loading-overlay" /> : null}
          </DataTable>
        )}

        {!loadError.failed && rankPage.totalCount > 0 ? (
          <Pagination
            className="problem-pagination submit-history-pagination"
            currentPage={rankPage.currentPage}
            totalPages={rankPage.totalPages}
            onPageChange={requestRankPage}
            ariaLabel={text('RANKING_PAGE_LABEL', '랭킹 페이지')}
            inputLabel={text('RANKING_PAGE_INPUT_LABEL', '이동할 랭킹 페이지 입력')}
            inputOpenLabel={text('RANKING_PAGE_INPUT_OPEN_LABEL', '이동할 랭킹 페이지 입력 열기')}
            previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
            nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
          />
        ) : null}
      </section>
      {linkMenuContent}
    </div>
  );
}
