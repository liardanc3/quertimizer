import { createPortal } from 'react-dom';
import { useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '../lib/favoriteTabs';
import { getLocationSearchSnapshot, getProfilePath, RANKING_PATH, subscribeLocation, navigate } from '../lib/navigation';
import { fetchRanks, type RankPage } from '../lib/rankApi';
import type { DbmsType, RankingMetricKey } from '../types/domain';
import './HomePage.css';
import './SubmitHistoryPage.css';
import './RankingPage.css';

const PAGE_SIZE = 100;

const dbmsOptions: Array<{ value: DbmsType; label: string }> = [
  { value: 'postgresql', label: 'PostgreSQL' },
  { value: 'oracle', label: 'Oracle' },
];

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

      try {
        const fetchedRankPage = await fetchRanks({
          page: requestedPage,
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
      } catch {
        if (cancelled) {
          return;
        }

        setLoadFailed(true);
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
      setRequestedPage(nextPage);
    }
  }

  function cancelPageJump() {
    setPageJumpDraft(String(rankPage.currentPage));
    setIsPageJumpEditing(false);
  }

  function openHandleMenu(handle: string, button: HTMLButtonElement) {
    const rect = button.getBoundingClientRect();
    const menuWidth = 224;
    const viewportWidth = document.documentElement.clientWidth;
    const maxLeft = viewportWidth - menuWidth - 12;
    const nextLeft = Math.max(12, Math.min(rect.left, maxLeft));

    setLinkMenuState({
      label: `${handle} 프로필로 이동`,
      path: getProfilePath(handle),
      ariaLabel: `${handle} 프로필 이동`,
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
        <div className="problem-toolbar submit-history-toolbar-stack ranking-toolbar-stack">
          <div className="solve-dbms-tab-row ranking-dbms-tab-row" role="tablist" aria-label="랭킹 DBMS 선택">
            {dbmsOptions.map((option) => {
              const isSelected = option.value === selectedDbms;

              return (
                <button
                  key={option.value}
                  type="button"
                  className={`solve-dbms-tab ${isSelected ? 'is-selected' : ''}`}
                  role="tab"
                  aria-selected={isSelected}
                  onClick={() => {
                    setSelectedDbms(option.value);
                    setRequestedPage(1);
                  }}
                >
                  {option.label}
                </button>
              );
            })}
            <FavoriteTabButton
              className="favorite-tab-toggle-end"
              label={`랭킹 / ${selectedDbms === 'oracle' ? 'Oracle' : 'PostgreSQL'}`}
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
            className="home-problem-search-form ranking-search-form"
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
                placeholder="Handle 검색"
                aria-label="Handle 검색"
              />
              <button type="submit" className="problem-search-button home-problem-search-button" aria-label="Handle 검색 실행">
                검색
              </button>
            </label>
          </form>
        </div>
      </section>

      <section className="panel-card problem-board submit-history-board ranking-board">
        {loadFailed ? (
          <PageLoadFailureState className="submit-history-empty-state" />
        ) : (
          <div className={`submit-history-table-shell ranking-table-shell ${isLoading ? 'is-loading' : ''}`}>
            <div className="submit-history-table ranking-table" role="table" aria-label="랭킹 목록">
              <div className="submit-history-row submit-history-head ranking-head" role="row">
                <div role="columnheader" className="submit-history-head-cell">순위</div>
                <div role="columnheader" className="submit-history-head-cell">Handle</div>
                <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                  <span>해결한 문제</span>
                  <button
                    type="button"
                    className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${sortKey === 'solvedCount' ? 'is-active' : ''}`}
                    aria-label="해결한 문제 내림차순 정렬"
                    onClick={() => {
                      if (sortKey !== 'solvedCount') {
                        setSortKey('solvedCount');
                        setRequestedPage(1);
                      }
                    }}
                  >
                    {sortKey === 'solvedCount' ? <SortDescendingIcon /> : <SortNeutralIcon />}
                  </button>
                </div>
                <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                  <span>평균 Cost 백분위</span>
                  <button
                    type="button"
                    className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${sortKey === 'avgExecutionPercentile' ? 'is-active' : ''}`}
                    aria-label="평균 Cost 백분위 오름차순 정렬"
                    onClick={() => {
                      if (sortKey !== 'avgExecutionPercentile') {
                        setSortKey('avgExecutionPercentile');
                        setRequestedPage(1);
                      }
                    }}
                  >
                    {sortKey === 'avgExecutionPercentile' ? <SortAscendingIcon /> : <SortNeutralIcon />}
                  </button>
                </div>
              </div>

              {rankedEntries.length === 0 && !isLoading ? (
                <div className="submit-history-row submit-history-empty-row" role="row">
                  <span className="submit-history-empty-cell" role="cell">
                    조건에 맞는 랭킹이 없습니다.
                  </span>
                </div>
              ) : (
                rankedEntries.map((entry) => (
                  <article key={`${selectedDbms}-${entry.handle}`} className="submit-history-row submit-history-body ranking-body" role="row">
                    <span className="submit-history-cell" role="cell" data-label="순위">
                      {entry.rank}
                    </span>
                    <span className="submit-history-cell" role="cell" data-label="Handle">
                      <button
                        type="button"
                        className="submit-history-link-button"
                        onClick={(event) => openHandleMenu(entry.handle, event.currentTarget)}
                        aria-label={`${entry.handle} Handle 메뉴 열기`}
                      >
                        {entry.handle}
                      </button>
                    </span>
                    <span className="submit-history-cell" role="cell" data-label="해결한 문제">
                      {entry.solvedCount}
                    </span>
                    <span className="submit-history-cell" role="cell" data-label="평균 Cost 백분위">
                      {formatPercentile(entry.avgExecutionPercentile)}
                    </span>
                  </article>
                ))
              )}
            </div>
            {isLoading ? (
              <div className="submit-history-loading-overlay" aria-live="polite" aria-label="로딩 중">
                <span className="page-loading-spinner submit-history-loading-badge" aria-hidden="true" />
              </div>
            ) : null}
          </div>
        )}

        {!loadFailed && rankPage.totalCount > 0 ? (
          <div className="problem-pagination submit-history-pagination" role="navigation" aria-label="랭킹 페이지">
            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => setRequestedPage((page) => Math.max(1, page - 1))}
              disabled={rankPage.currentPage === 1}
            >
              이전
            </button>

            {isPageJumpEditing ? (
              <input
                type="text"
                inputMode="numeric"
                className="problem-pagination-meta-input"
                aria-label="이동할 랭킹 페이지 입력"
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
                aria-label="이동할 랭킹 페이지 입력 열기"
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
              onClick={() => setRequestedPage((page) => Math.min(rankPage.totalPages, page + 1))}
              disabled={rankPage.currentPage >= rankPage.totalPages}
            >
              다음
            </button>
          </div>
        ) : null}
      </section>
      {linkMenuContent}
    </div>
  );
}
