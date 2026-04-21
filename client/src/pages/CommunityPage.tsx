import { useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '../lib/favoriteTabs';
import CommunityWritePage from './CommunityWritePage';
import { fetchCommunityPosts, type CommunityPostPage } from '../lib/communityApi';
import { COMMUNITY_PATH, COMMUNITY_WRITE_PATH, getCommunityPostPath, getLocationSearchSnapshot, getProfilePath, subscribeLocation, navigate } from '../lib/navigation';
import './HomePage.css';
import './SubmitHistoryPage.css';
import './CommunityPage.css';

type CommunitySortKey = 'default' | 'latest' | 'oldest' | 'views' | 'viewsAsc' | 'likes' | 'likesAsc' | 'comments' | 'commentsAsc';
type CommunityCategoryTab = 'all' | 'notice' | 'discussion' | 'question';

interface CommunityPageFavoriteSnapshot {
  draftSearchValue: string;
  searchQuery: string;
  selectedCategory: CommunityCategoryTab;
  sortKey: CommunitySortKey;
  requestedPage: number;
}

const PAGE_SIZE = 10;
const numberFormatter = new Intl.NumberFormat('ko-KR');
const loadingPlaceholderRows = Array.from({ length: 6 }, (_, index) => index);
const categoryTabs: Array<{ value: CommunityCategoryTab; label: string }> = [
  { value: 'all', label: '전체' },
  { value: 'notice', label: '공지' },
  { value: 'discussion', label: '자유' },
  { value: 'question', label: '질문' },
];

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

function isSortKey(value: string | null): value is CommunitySortKey {
  return value === 'default'
    || value === 'latest'
    || value === 'oldest'
    || value === 'views'
    || value === 'viewsAsc'
    || value === 'likes'
    || value === 'likesAsc'
    || value === 'comments'
    || value === 'commentsAsc';
}

function isCategoryTab(value: string | null): value is CommunityCategoryTab {
  return value === 'all' || value === 'notice' || value === 'discussion' || value === 'question';
}

function getDefaultSortKey(): CommunitySortKey {
  return 'default';
}

function readCommunityListState() {
  const params = new URLSearchParams(window.location.search);
  const search = params.get('search') ?? '';
  const sort = params.get('sort');
  const category = params.get('category');
  const page = Number.parseInt(params.get('page') ?? '1', 10);

  return {
    search,
    sortKey: isSortKey(sort) ? sort : getDefaultSortKey(),
    category: isCategoryTab(category) ? category : 'all',
    page: Number.isNaN(page) || page < 1 ? 1 : page,
  };
}

function getCommunityTabLabel(category: CommunityCategoryTab) {
  if (category === 'notice') {
    return '공지';
  }

  if (category === 'discussion') {
    return '자유';
  }

  if (category === 'question') {
    return '질문';
  }

  return '전체';
}

function buildCommunityFavoritePath(category: CommunityCategoryTab) {
  return category === 'all' ? COMMUNITY_PATH : `${COMMUNITY_PATH}?category=${encodeURIComponent(category)}`;
}

function buildCommunityWritePath(category: CommunityCategoryTab) {
  return category === 'all' ? COMMUNITY_WRITE_PATH : `${COMMUNITY_WRITE_PATH}?category=${encodeURIComponent(category)}`;
}

function buildCommunityListPath({ search, sortKey, category, page }: { search: string; sortKey: CommunitySortKey; category: CommunityCategoryTab; page: number }) {
  const params = new URLSearchParams();
  const normalizedSearch = search.trim();

  if (normalizedSearch !== '') {
    params.set('search', normalizedSearch);
  }

  if (sortKey !== getDefaultSortKey()) {
    params.set('sort', sortKey);
  }

  if (category !== 'all') {
    params.set('category', category);
  }

  if (page > 1) {
    params.set('page', String(page));
  }

  const query = params.toString();
  return query === '' ? COMMUNITY_PATH : `${COMMUNITY_PATH}?${query}`;
}

function formatNumber(value: number) {
  return numberFormatter.format(value);
}

function padDatePart(value: number) {
  return String(value).padStart(2, '0');
}

function formatBoardDate(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  const year = String(date.getFullYear());
  const month = padDatePart(date.getMonth() + 1);
  const day = padDatePart(date.getDate());
  const hours = padDatePart(date.getHours());
  const minutes = padDatePart(date.getMinutes());

  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function getCategoryLabel(value: string) {
  if (value === 'question') {
    return '질문';
  }

  if (value === 'notice') {
    return '공지';
  }

  if (value === 'tip') {
    return '팁';
  }

  return '자유';
}

const emptyPage: CommunityPostPage = {
  currentPage: 1,
  pageSize: PAGE_SIZE,
  totalCount: 0,
  totalPages: 1,
  posts: [],
};

export default function CommunityPage() {
  const locationSearch = useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
  const favoriteRestoreSnapshot = useMemo(() => readFavoriteRestoreSnapshot<CommunityPageFavoriteSnapshot>('community'), []);
  const didApplyFavoriteRestoreRef = useRef(false);
  const isWriteMode = window.location.pathname === COMMUNITY_WRITE_PATH;
  const initialState = favoriteRestoreSnapshot
    ? {
        search: favoriteRestoreSnapshot.searchQuery,
        sortKey: favoriteRestoreSnapshot.sortKey,
        category: favoriteRestoreSnapshot.selectedCategory,
        page: favoriteRestoreSnapshot.requestedPage,
      }
    : readCommunityListState();
  const [draftSearchValue, setDraftSearchValue] = useState(favoriteRestoreSnapshot?.draftSearchValue ?? initialState.search);
  const [searchQuery, setSearchQuery] = useState(initialState.search);
  const [selectedCategory, setSelectedCategory] = useState<CommunityCategoryTab>(initialState.category);
  const [sortKey, setSortKey] = useState<CommunitySortKey>(initialState.sortKey);
  const [requestedPage, setRequestedPage] = useState(initialState.page);
  const [isPageJumpEditing, setIsPageJumpEditing] = useState(false);
  const [pageJumpDraft, setPageJumpDraft] = useState(String(initialState.page));
  const [postPage, setPostPage] = useState<CommunityPostPage>(emptyPage);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    clearFavoriteRestoreSnapshot('community');
  }, []);

  useEffect(() => {
    const savedScrollY = window.history.state?.scrollY;

    if (typeof savedScrollY === 'number') {
      window.requestAnimationFrame(() => {
        window.scrollTo({ top: savedScrollY, behavior: 'auto' });
      });
    }
  }, []);

  useEffect(() => {
    const nextState = readCommunityListState();

    setDraftSearchValue((currentValue) => (currentValue === nextState.search ? currentValue : nextState.search));
    setSearchQuery((currentValue) => (currentValue === nextState.search ? currentValue : nextState.search));
    setSelectedCategory((currentValue) => (currentValue === nextState.category ? currentValue : nextState.category));
    setSortKey((currentValue) => (currentValue === nextState.sortKey ? currentValue : nextState.sortKey));
    setRequestedPage((currentValue) => (currentValue === nextState.page ? currentValue : nextState.page));
  }, [locationSearch]);

  useEffect(() => {
    if (isPageJumpEditing) {
      return;
    }

    setPageJumpDraft(String(postPage.currentPage));
  }, [isPageJumpEditing, postPage.currentPage]);

  useEffect(() => {
    if (isWriteMode) {
      setIsLoading(false);
      setErrorMessage(null);
      return;
    }

    let cancelled = false;

    setIsLoading(true);
    setErrorMessage(null);

    fetchCommunityPosts({
      page: requestedPage,
      search: searchQuery,
      tag: '',
      category: selectedCategory,
      sortKey,
    })
      .then((nextPostPage) => {
        if (cancelled) {
          return;
        }

        setPostPage(nextPostPage);
        if (requestedPage !== nextPostPage.currentPage) {
          setRequestedPage(nextPostPage.currentPage);
          replaceListHistory(searchQuery, selectedCategory, sortKey, nextPostPage.currentPage, 0);
        }
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }

        setErrorMessage(error instanceof Error ? error.message : '커뮤니티 게시글 조회에 실패했다.');
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [isWriteMode, requestedPage, searchQuery, selectedCategory, sortKey]);

  function replaceListHistory(nextSearch: string, nextCategory: CommunityCategoryTab, nextSortKey: CommunitySortKey, nextPage: number, scrollY = 0) {
    window.history.replaceState(
      { ...(window.history.state ?? {}), scrollY },
      '',
      buildCommunityListPath({ search: nextSearch, category: nextCategory, sortKey: nextSortKey, page: nextPage }),
    );
  }

  function moveList(nextSearch: string, nextCategory: CommunityCategoryTab, nextSortKey: CommunitySortKey, nextPage: number) {
    const trimmedSearch = nextSearch.trim();

    setSearchQuery(trimmedSearch);
    setSelectedCategory(nextCategory);
    setSortKey(nextSortKey);
    setRequestedPage(nextPage);
    replaceListHistory(trimmedSearch, nextCategory, nextSortKey, nextPage, 0);
    window.scrollTo({ top: 0, behavior: 'auto' });
  }

  function applySearch(value = draftSearchValue) {
    setDraftSearchValue(value);
    moveList(value, selectedCategory, sortKey, 1);
  }

  function selectCategory(category: CommunityCategoryTab) {
    if (!isWriteMode && category === selectedCategory) {
      return;
    }

    moveList(searchQuery, category, sortKey, 1);
  }

  function toggleMetricSort(descKey: CommunitySortKey, ascKey: CommunitySortKey) {
    const nextSortKey = sortKey === 'default'
      ? descKey
      : sortKey === descKey
        ? ascKey
        : sortKey === ascKey
          ? 'default'
          : descKey;

    moveList(searchQuery, selectedCategory, nextSortKey, 1);
  }

  function applyPageJump() {
    const parsedPage = Number.parseInt(pageJumpDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? postPage.currentPage
      : Math.min(postPage.totalPages, Math.max(1, parsedPage));

    setPageJumpDraft(String(nextPage));
    setIsPageJumpEditing(false);

    if (nextPage !== postPage.currentPage) {
      moveList(searchQuery, selectedCategory, sortKey, nextPage);
    }
  }

  function cancelPageJump() {
    setPageJumpDraft(String(postPage.currentPage));
    setIsPageJumpEditing(false);
  }

  useEffect(() => {
    if (!favoriteRestoreSnapshot || didApplyFavoriteRestoreRef.current || isWriteMode) {
      return;
    }

    didApplyFavoriteRestoreRef.current = true;
    setDraftSearchValue(favoriteRestoreSnapshot.draftSearchValue);
    setSearchQuery(favoriteRestoreSnapshot.searchQuery);
    setSelectedCategory(favoriteRestoreSnapshot.selectedCategory);
    setSortKey(favoriteRestoreSnapshot.sortKey);
    setRequestedPage(favoriteRestoreSnapshot.requestedPage);
    setPageJumpDraft(String(favoriteRestoreSnapshot.requestedPage));
    replaceListHistory(
      favoriteRestoreSnapshot.searchQuery,
      favoriteRestoreSnapshot.selectedCategory,
      favoriteRestoreSnapshot.sortKey,
      favoriteRestoreSnapshot.requestedPage,
      0,
    );
  }, [favoriteRestoreSnapshot, isWriteMode]);

  function handleOpenPost(postId: string) {
    const scrollY = window.scrollY;
    replaceListHistory(searchQuery, selectedCategory, sortKey, postPage.currentPage, scrollY);
    navigate(getCommunityPostPath(postId), {
      state: {
        fromListPath: buildCommunityListPath({ search: searchQuery, category: selectedCategory, sortKey, page: postPage.currentPage }),
        scrollY,
      },
    });
  }

  return (
    <div className="page-stack home-page community-page">
      <section className="panel-card compact community-toolbar-card">
        <div className="problem-toolbar community-toolbar submit-history-toolbar-stack">
          <div className="solve-dbms-tab-row community-tab-row" role="tablist" aria-label="커뮤니티 구분 선택">
            <div className="community-tab-list">
              {categoryTabs.map((tab) => {
                const isSelected = !isWriteMode && tab.value === selectedCategory;

                return (
                  <button
                    key={tab.value}
                    type="button"
                    className={`solve-dbms-tab ${isSelected ? 'is-selected' : ''}`}
                    role="tab"
                    aria-selected={isSelected}
                    onClick={() => selectCategory(tab.value)}
                  >
                    {tab.label}
                  </button>
                );
              })}

              <button
                type="button"
                className={`solve-dbms-tab ${isWriteMode ? 'is-selected' : ''}`}
                role="tab"
                aria-selected={isWriteMode}
                onClick={() => {
                  if (!isWriteMode) {
                    navigate(buildCommunityWritePath(selectedCategory));
                  }
                }}
              >
                글쓰기
              </button>
            </div>
            <FavoriteTabButton
              className="favorite-tab-toggle-end"
              label={`커뮤니티 / ${getCommunityTabLabel(selectedCategory)}`}
              path={buildCommunityListPath({ search: searchQuery, sortKey, category: selectedCategory, page: requestedPage })}
              snapshot={{
                kind: 'community',
                payload: {
                  draftSearchValue,
                  searchQuery,
                  selectedCategory,
                  sortKey,
                  requestedPage,
                },
              }}
            />
          </div>

          {!isWriteMode ? (
            <form
              className="problem-search-form home-problem-search-form community-search-form"
              onSubmit={(event) => {
                event.preventDefault();
                applySearch();
              }}
            >
              <label className="problem-search-field home-problem-search-field community-search-field">
                <input
                  type="search"
                  value={draftSearchValue}
                  onChange={(event) => setDraftSearchValue(event.target.value)}
                  className="text-field problem-search-input home-problem-search-input community-search-input"
                  placeholder="제목, 작성자, 태그, 내용 검색"
                  aria-label="커뮤니티 검색"
                />
                <button type="submit" className="problem-search-button home-problem-search-button" aria-label="커뮤니티 검색 실행">
                  검색
                </button>
              </label>
            </form>
          ) : null}
        </div>
      </section>

      {isWriteMode ? (
        <section className="panel-card problem-board community-board community-write-board">
          <CommunityWritePage embedded />
        </section>
      ) : (
      <section className="panel-card problem-board community-board">
        {errorMessage ? (
          <PageLoadFailureState className="submit-history-empty-state" />
        ) : (
          <div className={`submit-history-table-shell community-table-shell ${isLoading ? 'is-loading' : ''}`.trim()}>
            <div className="submit-history-table community-table" role="table" aria-label="커뮤니티 목록">
              <div className="submit-history-row submit-history-head community-table-head" role="row">
                <div role="columnheader" className="submit-history-head-cell">구분</div>
                <div role="columnheader" className="submit-history-head-cell">제목</div>
                <div role="columnheader" className="submit-history-head-cell">Handle</div>
                <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                  <span>작성일</span>
                  <button
                    type="button"
                    className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${(sortKey === 'latest' || sortKey === 'oldest') ? 'is-active' : ''}`.trim()}
                    aria-label={sortKey === 'oldest' ? '작성일 오름차순 정렬' : sortKey === 'latest' ? '작성일 내림차순 정렬' : '작성일 기본 정렬'}
                    onClick={() => toggleMetricSort('latest', 'oldest')}
                  >
                    {sortKey === 'oldest' ? <SortAscendingIcon /> : sortKey === 'latest' ? <SortDescendingIcon /> : <SortNeutralIcon />}
                  </button>
                </div>
                <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                  <span>조회수</span>
                  <button
                    type="button"
                    className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${(sortKey === 'views' || sortKey === 'viewsAsc') ? 'is-active' : ''}`.trim()}
                    aria-label={sortKey === 'viewsAsc' ? '조회수 오름차순 정렬' : sortKey === 'views' ? '조회수 내림차순 정렬' : '조회수 기본 정렬'}
                    onClick={() => toggleMetricSort('views', 'viewsAsc')}
                  >
                    {sortKey === 'viewsAsc' ? <SortAscendingIcon /> : sortKey === 'views' ? <SortDescendingIcon /> : <SortNeutralIcon />}
                  </button>
                </div>
                <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                  <span>좋아요</span>
                  <button
                    type="button"
                    className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${(sortKey === 'likes' || sortKey === 'likesAsc') ? 'is-active' : ''}`.trim()}
                    aria-label={sortKey === 'likesAsc' ? '좋아요 오름차순 정렬' : sortKey === 'likes' ? '좋아요 내림차순 정렬' : '좋아요 기본 정렬'}
                    onClick={() => toggleMetricSort('likes', 'likesAsc')}
                  >
                    {sortKey === 'likesAsc' ? <SortAscendingIcon /> : sortKey === 'likes' ? <SortDescendingIcon /> : <SortNeutralIcon />}
                  </button>
                </div>
                <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                  <span>댓글</span>
                  <button
                    type="button"
                    className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${(sortKey === 'comments' || sortKey === 'commentsAsc') ? 'is-active' : ''}`.trim()}
                    aria-label={sortKey === 'commentsAsc' ? '댓글 수 오름차순 정렬' : sortKey === 'comments' ? '댓글 수 내림차순 정렬' : '댓글 수 기본 정렬'}
                    onClick={() => toggleMetricSort('comments', 'commentsAsc')}
                  >
                    {sortKey === 'commentsAsc' ? <SortAscendingIcon /> : sortKey === 'comments' ? <SortDescendingIcon /> : <SortNeutralIcon />}
                  </button>
                </div>
              </div>

              {isLoading && postPage.posts.length === 0 ? (
                loadingPlaceholderRows.map((rowIndex) => (
                  <div key={`community-loading-${rowIndex}`} className="submit-history-row submit-history-body community-table-row community-loading-row" role="row" aria-hidden="true">
                    <div role="cell" className="submit-history-cell community-table-cell"><span className="community-loading-placeholder is-short" /></div>
                    <div role="cell" className="submit-history-cell community-table-cell community-table-title-cell">
                      <span className="community-loading-placeholder is-long" />
                      <span className="community-loading-placeholder is-medium" />
                    </div>
                    <div role="cell" className="submit-history-cell community-table-cell"><span className="community-loading-placeholder is-medium" /></div>
                    <div role="cell" className="submit-history-cell community-table-cell"><span className="community-loading-placeholder is-medium" /></div>
                    <div role="cell" className="submit-history-cell community-table-cell"><span className="community-loading-placeholder is-short" /></div>
                    <div role="cell" className="submit-history-cell community-table-cell"><span className="community-loading-placeholder is-short" /></div>
                    <div role="cell" className="submit-history-cell community-table-cell"><span className="community-loading-placeholder is-short" /></div>
                  </div>
                ))
              ) : postPage.posts.length === 0 ? (
                <div className="submit-history-row submit-history-empty-row community-empty-row" role="row">
                  <span className="submit-history-empty-cell" role="cell">조건에 맞는 게시글이 없습니다.</span>
                </div>
              ) : (
                postPage.posts.map((post) => (
                  <article key={post.id} className="submit-history-row submit-history-body community-table-row" role="row">
                    <div role="cell" className="submit-history-cell community-table-cell" data-label="구분">
                      <span className={`community-category-text is-${post.category}`}>{getCategoryLabel(post.category)}</span>
                    </div>

                    <div role="cell" className="submit-history-cell community-table-cell community-table-title-cell" data-label="제목">
                      <button type="button" className="community-post-title-link" onClick={() => handleOpenPost(post.id)}>
                        <span className="community-post-title-text">{post.title}</span>
                      </button>

                      {post.tags.length > 0 ? (
                        <div className="community-table-tags">
                          {Array.from(new Set(post.tags)).slice(0, 5).map((tag) => (
                            <span key={tag} className="community-table-tag">#{tag}</span>
                          ))}
                        </div>
                      ) : null}
                    </div>

                    <div role="cell" className="submit-history-cell community-table-cell" data-label="Handle">
                      <button
                        type="button"
                        className="community-handle-link"
                        onClick={() => navigate(getProfilePath(post.authorHandle))}
                      >
                        {post.authorHandle}
                      </button>
                    </div>

                    <div role="cell" className="submit-history-cell community-table-cell community-table-date-cell" data-label="작성일">
                      {formatBoardDate(post.updatedAt ?? post.createdAt)}
                    </div>

                    <div role="cell" className="submit-history-cell community-table-cell" data-label="조회수">
                      {formatNumber(post.views)}
                    </div>

                    <div role="cell" className="submit-history-cell community-table-cell" data-label="좋아요">
                      {formatNumber(post.likes)}
                    </div>

                    <div role="cell" className="submit-history-cell community-table-cell" data-label="댓글">
                      {formatNumber(post.comments)}
                    </div>
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

        {errorMessage == null && postPage.totalCount > 0 ? (
          <div className="problem-pagination submit-history-pagination" role="navigation" aria-label="커뮤니티 페이지">
            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => moveList(searchQuery, selectedCategory, sortKey, Math.max(1, postPage.currentPage - 1))}
              disabled={postPage.currentPage === 1}
            >
              이전
            </button>

            {isPageJumpEditing ? (
              <input
                type="text"
                inputMode="numeric"
                className="problem-pagination-meta-input"
                aria-label="이동할 커뮤니티 페이지 입력"
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
                aria-label="이동할 커뮤니티 페이지 입력 열기"
                onClick={() => {
                  setPageJumpDraft(String(postPage.currentPage));
                  setIsPageJumpEditing(true);
                }}
              >
                {`${postPage.currentPage} / ${postPage.totalPages}`}
              </button>
            )}

            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => moveList(searchQuery, selectedCategory, sortKey, Math.min(postPage.totalPages, postPage.currentPage + 1))}
              disabled={postPage.currentPage >= postPage.totalPages}
            >
              다음
            </button>
          </div>
        ) : null}
      </section>
      )}
    </div>
  );
}
