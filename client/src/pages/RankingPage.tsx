import { createPortal } from 'react-dom';
import { useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import HttpErrorState from '../components/common/HttpErrorState';
import { LoadingOverlay } from '../components/common/LoadingSpinner';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { getApiErrorStatus, isCommonHttpErrorStatus } from '../lib/apiError';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '../lib/favoriteTabs';
import { getLocationSearchSnapshot, getProfilePath, RANKING_PATH, subscribeLocation, navigate } from '../lib/navigation';
import { fetchRanks, type RankPage } from '../lib/rankApi';
import { useUiText } from '../lib/uiText';
import type { DbmsType, RankingMetricKey } from '../types/domain';
import './HomePage.css';
import './SubmitHistoryPage.css';
import './RankingPage.css';

const PAGE_SIZE = 10;
const rankingLoadingRows = Array.from({ length: 10 }, (_, index) => index);

const dbmsOptions: DbmsType[] = ['postgresql', 'oracle'];

function readRankingDbmsFromSearch(search: string) {
  const dbms = new URLSearchParams(search).get('dbms');
  return dbms === 'oracle' ? 'oracle' : 'postgresql';
}

function buildRankingPath(dbms: DbmsType) {
  if (dbms === 'postgresql') {
    return RANKING_PATH;
  }

  return `${RANKING_PATH}?dbms=${encodeURIComponent(dbms)}`;
}

function SortAscendingIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 2.5v10.9" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      <path d="M5.2 5.25 8 2.5l2.8 2.75" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function SortDescendingIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 2.6v10.9" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      <path d="m5.2 10.75 2.8 2.75 2.8-2.75" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function SortNeutralIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M5.7 6.2 8 3.9l2.3 2.3" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      <path d="m5.7 9.8 2.3 2.3 2.3-2.3" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
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

function formatPercentile(value: number) {
  return `${value.toFixed(1)}%`;
}

export default function RankingPage() {
  const { text } = useUiText();
  const locationSearch = useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
  const favoriteRestoreSnapshot = useMemo(() => readFavoriteRestoreSnapshot<RankingPageFavoriteSnapshot>('ranking'), []);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>(() => favoriteRestoreSnapshot?.selectedDbms ?? readRankingDbmsFromSearch(window.location.search));
  const [sortKey, setSortKey] = useState<RankingMetricKey>(() => favoriteRestoreSnapshot?.sortKey ?? 'solvedCount');
  const [draftQuery, setDraftQuery] = useState(() => favoriteRestoreSnapshot?.draftQuery ?? '');
  const [submittedQuery, setSubmittedQuery] = useState(() => favoriteRestoreSnapshot?.submittedQuery ?? '');
  const [requestedPage, setRequestedPage] = useState(() => favoriteRestoreSnapshot?.requestedPage ?? 1);
  const [isPageJumpEditing, setIsPageJumpEditing] = useState(false);
  const [pageJumpDraft, setPageJumpDraft] = useState('1');
  const [rankPage, setRankPage] = useState<RankPage>(createEmptyRankPage());
  const [isLoading, setIsLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [loadErrorMessage, setLoadErrorMessage] = useState<string | null>(null);
  const [loadErrorStatus, setLoadErrorStatus] = useState<number | null>(null);
  const [linkMenuState, setLinkMenuState] = useState<LinkMenuState>(null);
  const linkMenuRef = useRef<HTMLButtonElement | null>(null);

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
    if (isPageJumpEditing) {
      return;
    }

    setPageJumpDraft(String(rankPage.currentPage));
  }, [isPageJumpEditing, rankPage.currentPage]);

  useEffect(() => {
    const nextDbms = readRankingDbmsFromSearch(locationSearch);

    setSelectedDbms((currentDbms) => (currentDbms === nextDbms ? currentDbms : nextDbms));
  }, [locationSearch]);

  useEffect(() => {
    const nextPath = buildRankingPath(selectedDbms);
    const currentPath = `${window.location.pathname}${window.location.search}`;

    if (currentPath !== nextPath) {
      window.history.replaceState(window.history.state ?? {}, '', nextPath);
    }
  }, [selectedDbms]);

  useEffect(() => {
    let cancelled = false;

    async function loadRanks() {
      setIsLoading(true);
      setLoadFailed(false);
      setLoadErrorMessage(null);
      setLoadErrorStatus(null);

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

        setLoadFailed(true);
        setLoadErrorMessage(error instanceof Error ? error.message : text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
        const status = getApiErrorStatus(error);
        setLoadErrorStatus(isCommonHttpErrorStatus(status) ? status : null);
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

  useEffect(() => {
    if (linkMenuState == null) {
      return;
    }

    function closeLinkMenu() {
      setLinkMenuState(null);
    }

    function handlePointerDown(event: MouseEvent) {
      if (!linkMenuRef.current?.contains(event.target as Node)) {
        closeLinkMenu();
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        closeLinkMenu();
      }
    }

    window.addEventListener('mousedown', handlePointerDown);
    window.addEventListener('keydown', handleEscape);
    window.addEventListener('resize', closeLinkMenu);
    window.addEventListener('scroll', closeLinkMenu, true);

    return () => {
      window.removeEventListener('mousedown', handlePointerDown);
      window.removeEventListener('keydown', handleEscape);
      window.removeEventListener('resize', closeLinkMenu);
      window.removeEventListener('scroll', closeLinkMenu, true);
    };
  }, [linkMenuState]);

  function applySearch() {
    const willReload = draftQuery.trim() !== submittedQuery.trim() || requestedPage !== 1;
    if (!willReload) {
      return;
    }

    setIsLoading(true);
    setSubmittedQuery(draftQuery);
    setRequestedPage(1);
  }

  function applyPageJump() {
    const parsedPage = Number.parseInt(pageJumpDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? rankPage.currentPage
      : Math.min(rankPage.totalPages, Math.max(1, parsedPage));

    setPageJumpDraft(String(nextPage));
    setIsPageJumpEditing(false);

    if (nextPage !== rankPage.currentPage) {
      requestRankPage(nextPage);
    }
  }

  function cancelPageJump() {
    setPageJumpDraft(String(rankPage.currentPage));
    setIsPageJumpEditing(false);
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
    <div className="page-stack ranking-page submit-history-page home-page">
      <section className="panel-card compact problem-toolbar-card submit-history-toolbar-card ranking-toolbar-card">
        <div className="problem-toolbar home-problem-toolbar-stack submit-history-toolbar-stack ranking-toolbar-stack">
          <div className="solve-dbms-tab-row ranking-dbms-tab-row" role="tablist" aria-label={text('RANKING_DBMS_TABLIST_LABEL', '랭킹 DBMS 선택')}>
            {dbmsOptions.map((dbmsOption) => {
              const isSelected = dbmsOption === selectedDbms;
              const dbmsLabel = dbmsOption === 'oracle' ? text('COMMON_ORACLE_LABEL', 'Oracle') : text('COMMON_POSTGRESQL_LABEL', 'PostgreSQL');

              return (
                <button
                  key={dbmsOption}
                  type="button"
                  className={`solve-dbms-tab ${isSelected ? 'is-selected' : ''}`}
                  role="tab"
                  aria-selected={isSelected}
                  onClick={() => {
                    if (!isSelected) {
                      setIsLoading(true);
                      setSelectedDbms(dbmsOption);
                      setRequestedPage(1);
                    }
                  }}
                >
                  {dbmsLabel}
                </button>
              );
            })}
            <FavoriteTabButton
              className="favorite-tab-toggle-end"
              label={text(
                'RANKING_FAVORITE_LABEL',
                { dbms: selectedDbms === 'oracle' ? text('COMMON_ORACLE_LABEL', 'Oracle') : text('COMMON_POSTGRESQL_LABEL', 'PostgreSQL') },
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
          </div>

          <form
            className="problem-search-form home-problem-search-form ranking-search-form"
            onSubmit={(event) => {
              event.preventDefault();
              applySearch();
            }}
          >
            <label className="problem-search-field home-problem-search-field ranking-search-field">
              <input
                type="search"
                value={draftQuery}
                onChange={(event) => setDraftQuery(event.target.value)}
                className="text-field problem-search-input home-problem-search-input ranking-search-input"
                placeholder={text('RANKING_HANDLE_SEARCH_PLACEHOLDER', 'Handle 검색')}
                aria-label={text('RANKING_HANDLE_SEARCH_LABEL', 'Handle 검색')}
              />
              <button type="submit" className="problem-search-button home-problem-search-button" aria-label={text('RANKING_HANDLE_SEARCH_SUBMIT_LABEL', 'Handle 검색 실행')}>
                {text('COMMON_SEARCH_BUTTON', '검색')}
              </button>
            </label>
          </form>
        </div>
      </section>

      <section className="panel-card problem-board submit-history-board ranking-board">
        {loadFailed ? (
          loadErrorStatus != null
            ? <HttpErrorState status={loadErrorStatus} message={loadErrorMessage} />
            : <PageLoadFailureState message={loadErrorMessage} />
        ) : (
          <div className={`submit-history-table ranking-table ${isLoading ? 'is-loading' : ''}`} role="table" aria-label={text('RANKING_TABLE_LABEL', '랭킹 목록')}>
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
                  {sortKey === 'solvedCount' ? <SortDescendingIcon /> : <SortNeutralIcon />}
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
                  {sortKey === 'avgExecutionPercentile' ? <SortAscendingIcon /> : <SortNeutralIcon />}
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
                    {formatPercentile(entry.avgExecutionPercentile)}
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
          </div>
        )}

        {!loadFailed && rankPage.totalCount > 0 ? (
          <div className="problem-pagination submit-history-pagination" role="navigation" aria-label={text('RANKING_PAGE_LABEL', '랭킹 페이지')}>
            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => requestRankPage(rankPage.currentPage - 1)}
              disabled={rankPage.currentPage === 1}
            >
              {text('COMMON_PREVIOUS_BUTTON', '이전')}
            </button>

            {isPageJumpEditing ? (
              <input
                type="text"
                inputMode="numeric"
                className="problem-pagination-meta-input"
                aria-label={text('RANKING_PAGE_INPUT_LABEL', '이동할 랭킹 페이지 입력')}
                value={pageJumpDraft}
                onChange={(event) => setPageJumpDraft(event.target.value.replace(/\D+/g, ''))}
                onBlur={applyPageJump}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    applyPageJump();
                    return;
                  }

                  if (event.key === 'Escape') {
                    event.preventDefault();
                    cancelPageJump();
                  }
                }}
                autoFocus
              />
            ) : (
              <button
                type="button"
                className="problem-pagination-meta problem-pagination-meta-button"
                aria-label={text('RANKING_PAGE_INPUT_OPEN_LABEL', '이동할 랭킹 페이지 입력 열기')}
                onClick={() => {
                  setPageJumpDraft(String(rankPage.currentPage));
                  setIsPageJumpEditing(true);
                }}
              >
                {`${rankPage.currentPage} / ${rankPage.totalPages}`}
              </button>
            )}

            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => requestRankPage(rankPage.currentPage + 1)}
              disabled={rankPage.currentPage >= rankPage.totalPages}
            >
              {text('COMMON_NEXT_BUTTON', '다음')}
            </button>
          </div>
        ) : null}
      </section>
      {linkMenuContent}
    </div>
  );
}
