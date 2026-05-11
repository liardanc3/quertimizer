import { lazy, Suspense, type KeyboardEvent, useEffect, useMemo, useRef, useState } from 'react';
import { FavoriteTabButton } from '@/features/favorite-tab';
import { DataTable } from '@/shared/ui';
import { ContentLoading } from '@/shared/ui';
import { PageErrorState } from '@/shared/ui';
import { Pagination } from '@/shared/ui';
import { SearchForm } from '@/shared/ui';
import { SortIcon } from '@/shared/ui/icons';
import { useLocationSearch } from '@/shared/lib/hooks/use-location-state';
import useDismissableLayer from '@/shared/lib/hooks/use-dismissable-layer';
import useRequestState from '@/shared/lib/hooks/use-request-state';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '@/features/favorite-tab';
import { fetchCommunityPosts, type CommunityPostPage } from '@/shared/api/community-api';
import { COMMUNITY_PATH, COMMUNITY_WRITE_PATH, getCommunityPostPath, getProfilePath, navigate } from '@/shared/config/navigation';
import { useSession } from '@/shared/auth/session';
import { getUiTextValue, useUiText } from '@/shared/config/ui-text';
import { formatBoardDate, formatInteger } from '@/shared/lib/formatters';
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
const CommunityWritePage = lazy(() => import('@/pages/community/ui/CommunityWritePage'));

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
    return getUiTextValue('COMMUNITY_CATEGORY_NOTICE_LABEL', '공지');
  }

  if (category === 'discussion') {
    return getUiTextValue('COMMUNITY_CATEGORY_FREE_LABEL', '자유');
  }

  if (category === 'question') {
    return getUiTextValue('COMMUNITY_CATEGORY_QUESTION_LABEL', '질문');
  }

  return getUiTextValue('COMMUNITY_CATEGORY_ALL_LABEL', '전체');
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

function getCategoryLabel(value: string) {
  if (value === 'question') {
    return getUiTextValue('COMMUNITY_CATEGORY_QUESTION_LABEL', '질문');
  }

  if (value === 'notice') {
    return getUiTextValue('COMMUNITY_CATEGORY_NOTICE_LABEL', '공지');
  }

  if (value === 'tip') {
    return getUiTextValue('COMMUNITY_CATEGORY_TIP_LABEL', '팁');
  }

  return getUiTextValue('COMMUNITY_CATEGORY_FREE_LABEL', '자유');
}

function ViewIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M2.4 8s1.9-3.7 5.6-3.7S13.6 8 13.6 8 11.7 11.7 8 11.7 2.4 8 2.4 8Z" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M8 9.75A1.75 1.75 0 1 0 8 6.25a1.75 1.75 0 0 0 0 3.5Z" stroke="currentColor" strokeWidth="1.35" />
    </svg>
  );
}

function LikeIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 13.3 3.5 9.1a2.8 2.8 0 0 1 4-4L8 5.6l.5-.5a2.8 2.8 0 0 1 4 4L8 13.3Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function CommentIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M3.5 3.9h9v6.1H7.4l-3.1 2.35V10h-.8V3.9Z" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

const emptyPage: CommunityPostPage = {
  currentPage: 1,
  pageSize: PAGE_SIZE,
  totalCount: 0,
  totalPages: 1,
  posts: [],
};

function ProblemFilterIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path d="M3.5 5.5h13M6 10h8M8.5 14.5h3" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  );
}

export default function CommunityPage() {
  const { text } = useUiText();
  const { isAuthenticated } = useSession();
  const locationSearch = useLocationSearch();
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
  const [isMobileSortOpen, setIsMobileSortOpen] = useState(false);
  const mobileSortRef = useRef<HTMLDivElement | null>(null);
  const mobileSortLayerRefs = useMemo(() => [mobileSortRef], []);
  const {
    data: postPage,
    setData: setPostPage,
    isLoading,
    setIsLoading,
    error: loadError,
    beginRequest,
    failRequest,
    resetError,
  } = useRequestState<CommunityPostPage>(() => emptyPage);
  const categoryTabs: Array<{ value: CommunityCategoryTab; label: string }> = useMemo(
    () => [
      { value: 'all', label: text('COMMUNITY_CATEGORY_ALL_LABEL', '전체') },
      { value: 'notice', label: text('COMMUNITY_CATEGORY_NOTICE_LABEL', '공지') },
      { value: 'discussion', label: text('COMMUNITY_CATEGORY_FREE_LABEL', '자유') },
      { value: 'question', label: text('COMMUNITY_CATEGORY_QUESTION_LABEL', '질문') },
    ],
    [text],
  );

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

  function replaceListHistory(nextSearch: string, nextCategory: CommunityCategoryTab, nextSortKey: CommunitySortKey, nextPage: number, scrollY = 0) {
    window.history.replaceState(
      { ...(window.history.state ?? {}), scrollY },
      '',
      buildCommunityListPath({ search: nextSearch, category: nextCategory, sortKey: nextSortKey, page: nextPage }),
    );
  }

  useEffect(() => {
    if (isWriteMode) {
      setIsLoading(false);
      resetError();
      return;
    }

    let cancelled = false;

    beginRequest();

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

        failRequest(error, text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
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

  function moveList(nextSearch: string, nextCategory: CommunityCategoryTab, nextSortKey: CommunitySortKey, nextPage: number) {
    const trimmedSearch = nextSearch.trim();
    const isSameRequest =
      trimmedSearch === searchQuery
      && nextCategory === selectedCategory
      && nextSortKey === sortKey
      && nextPage === requestedPage;

    if (isSameRequest) {
      return;
    }

    setIsLoading(true);
    resetError();
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

    setIsLoading(true);
    navigate(buildCommunityListPath({ search: searchQuery, category, sortKey, page: 1 }));
    window.scrollTo({ top: 0, behavior: 'auto' });
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

  function renderMobileSortRow(label: string, descKey: CommunitySortKey, ascKey: CommunitySortKey) {
    return (
      <div className="problem-mobile-sort-row">
        <span className="problem-mobile-sort-label">{label}</span>
        <div className="problem-mobile-sort-options">
          <label className="problem-mobile-sort-option">
            <input
              type="radio"
              name={`community-mobile-sort-${descKey}`}
              checked={sortKey === descKey}
              onChange={() => moveList(searchQuery, selectedCategory, descKey, 1)}
              aria-label={text('COMMUNITY_MOBILE_SORT_DESC_LABEL', { label }, `${label} 내림차순`)}
            />
            <span className="problem-status-check-ui" aria-hidden="true" />
            <span>{text('COMMON_DESC_SORT_FULL_LABEL', '내림차순')}</span>
          </label>

          <label className="problem-mobile-sort-option">
            <input
              type="radio"
              name={`community-mobile-sort-${descKey}`}
              checked={sortKey === ascKey}
              onChange={() => moveList(searchQuery, selectedCategory, ascKey, 1)}
              aria-label={text('COMMUNITY_MOBILE_SORT_ASC_LABEL', { label }, `${label} 오름차순`)}
            />
            <span className="problem-status-check-ui" aria-hidden="true" />
            <span>{text('COMMON_ASC_SORT_FULL_LABEL', '오름차순')}</span>
          </label>
        </div>
      </div>
    );
  }

  useDismissableLayer({
    enabled: isMobileSortOpen,
    refs: mobileSortLayerRefs,
    onDismiss: () => setIsMobileSortOpen(false),
    dismissOnResize: true,
  });

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
    setIsLoading(true);
    replaceListHistory(searchQuery, selectedCategory, sortKey, postPage.currentPage, scrollY);
    navigate(getCommunityPostPath(postId), {
      state: {
        fromListPath: buildCommunityListPath({ search: searchQuery, category: selectedCategory, sortKey, page: postPage.currentPage }),
        scrollY,
      },
    });
  }

  function handlePostRowKeyDown(event: KeyboardEvent<HTMLElement>, postId: string) {
    if (event.key !== 'Enter' && event.key !== ' ') {
      return;
    }

    event.preventDefault();
    handleOpenPost(postId);
  }

  return (
    <div className="page page-stack data-page community-page">
      <section className="panel-card compact community-toolbar-card">
        <div className="problem-toolbar community-toolbar submit-history-toolbar-stack">
          <div className="solve-dbms-tab-row community-tab-row tab-row" role="tablist" aria-label={text('COMMUNITY_TABLIST_LABEL', '커뮤니티 구분 선택')}>
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
                className={`solve-dbms-tab ${isWriteMode ? 'is-selected' : ''} ${!isAuthenticated ? 'is-disabled' : ''}`.trim()}
                role="tab"
                aria-selected={isWriteMode}
                disabled={!isAuthenticated}
                title={isAuthenticated ? undefined : text('COMMUNITY_LOGIN_REQUIRED_TOOLTIP', '로그인 후 글쓰기를 사용할 수 있습니다.')}
                onClick={() => {
                  if (!isWriteMode) {
                    navigate(buildCommunityWritePath(selectedCategory));
                  }
                }}
              >
                {text('COMMUNITY_WRITE_BUTTON', '글쓰기')}
              </button>
            </div>
            <FavoriteTabButton
              className="favorite-tab-toggle-end"
              label={text('COMMUNITY_FAVORITE_LABEL', { category: getCommunityTabLabel(selectedCategory) }, `커뮤니티 / ${getCommunityTabLabel(selectedCategory)}`)}
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
            <div className="home-problem-search-row community-search-row" ref={mobileSortRef}>
              <SearchForm
                className="problem-search-form home-problem-search-form community-search-form"
                fieldClassName="problem-search-field home-problem-search-field community-search-field"
                inputClassName="problem-search-input home-problem-search-input community-search-input"
                buttonClassName="problem-search-button home-problem-search-button"
                value={draftSearchValue}
                onChange={setDraftSearchValue}
                onSubmit={applySearch}
                placeholder={text('COMMUNITY_SEARCH_PLACEHOLDER', '제목, 작성자, 태그, 내용 검색')}
                label={text('COMMUNITY_SEARCH_LABEL', '커뮤니티 검색')}
                submitLabel={text('COMMUNITY_SEARCH_SUBMIT_LABEL', '커뮤니티 검색 실행')}
                buttonLabel={text('COMMON_SEARCH_BUTTON', '검색')}
              />

              <button
                type="button"
                className={`problem-mobile-filter-button community-mobile-sort-button ${isMobileSortOpen ? 'is-open' : ''} ${sortKey !== 'default' ? 'is-active' : ''}`.trim()}
                aria-label={text('COMMUNITY_MOBILE_SORT_OPEN_LABEL', '커뮤니티 정렬 필터 열기')}
                aria-expanded={isMobileSortOpen}
                onClick={() => setIsMobileSortOpen((value) => !value)}
              >
                <ProblemFilterIcon />
              </button>

              {isMobileSortOpen ? (
                <div className="problem-mobile-filter-menu community-mobile-sort-menu" role="dialog" aria-label={text('COMMUNITY_MOBILE_SORT_MENU_LABEL', '커뮤니티 정렬 필터')}>
                  <div className="problem-mobile-sort-list">
                    {renderMobileSortRow(text('COMMUNITY_DATE_COLUMN_LABEL', '작성일'), 'latest', 'oldest')}
                    {renderMobileSortRow(text('COMMUNITY_VIEWS_COLUMN_LABEL', '조회수'), 'views', 'viewsAsc')}
                    {renderMobileSortRow(text('COMMUNITY_LIKES_COLUMN_LABEL', '좋아요'), 'likes', 'likesAsc')}
                    {renderMobileSortRow(text('COMMUNITY_COMMENTS_COLUMN_LABEL', '댓글'), 'comments', 'commentsAsc')}
                  </div>
                </div>
              ) : null}
            </div>
          ) : null}
        </div>
      </section>

      {isWriteMode ? (
        <section className="panel-card problem-board community-board community-write-board data-board">
          <Suspense fallback={<ContentLoading className="community-board-loading" label={text('COMMUNITY_TABLE_LOADING_LABEL', '로딩 중')} />}>
            <CommunityWritePage embedded />
          </Suspense>
        </section>
      ) : (
        <section className="panel-card problem-board community-board data-board">
        {isLoading ? (
          <ContentLoading className="community-board-loading" label={text('COMMUNITY_TABLE_LOADING_LABEL', '로딩 중')} />
        ) : loadError.failed ? (
          <PageErrorState status={loadError.status} message={loadError.message} />
        ) : (
          <div className="community-table-loading-shell">
            <div className="submit-history-table-shell community-table-shell">
              <DataTable className="submit-history-table community-table" label={text('COMMUNITY_TABLE_LABEL', '커뮤니티 목록')}>
                <div className="submit-history-row submit-history-head community-table-head" role="row">
                  <div role="columnheader" className="submit-history-head-cell">{text('COMMUNITY_CATEGORY_COLUMN_LABEL', '구분')}</div>
                  <div role="columnheader" className="submit-history-head-cell">{text('COMMUNITY_TITLE_COLUMN_LABEL', '제목')}</div>
                  <div role="columnheader" className="submit-history-head-cell">{text('COMMON_HANDLE_LABEL', 'Handle')}</div>
                  <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                    <span>{text('COMMUNITY_DATE_COLUMN_LABEL', '작성일')}</span>
                    <button
                      type="button"
                      className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${(sortKey === 'latest' || sortKey === 'oldest') ? 'is-active' : ''}`.trim()}
                      aria-label={
                        sortKey === 'oldest'
                          ? text('COMMUNITY_DATE_SORT_ASC_LABEL', '작성일 오름차순 정렬')
                          : sortKey === 'latest'
                            ? text('COMMUNITY_DATE_SORT_DESC_LABEL', '작성일 내림차순 정렬')
                            : text('COMMUNITY_DATE_SORT_DEFAULT_LABEL', '작성일 기본 정렬')
                      }
                      onClick={() => toggleMetricSort('latest', 'oldest')}
                    >
                      <SortIcon direction={sortKey === 'oldest' ? 'asc' : sortKey === 'latest' ? 'desc' : 'none'} />
                    </button>
                  </div>
                  <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                    <span>{text('COMMUNITY_VIEWS_COLUMN_LABEL', '조회수')}</span>
                    <button
                      type="button"
                      className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${(sortKey === 'views' || sortKey === 'viewsAsc') ? 'is-active' : ''}`.trim()}
                      aria-label={
                        sortKey === 'viewsAsc'
                          ? text('COMMUNITY_VIEWS_SORT_ASC_LABEL', '조회수 오름차순 정렬')
                          : sortKey === 'views'
                            ? text('COMMUNITY_VIEWS_SORT_DESC_LABEL', '조회수 내림차순 정렬')
                            : text('COMMUNITY_VIEWS_SORT_DEFAULT_LABEL', '조회수 기본 정렬')
                      }
                      onClick={() => toggleMetricSort('views', 'viewsAsc')}
                    >
                      <SortIcon direction={sortKey === 'viewsAsc' ? 'asc' : sortKey === 'views' ? 'desc' : 'none'} />
                    </button>
                  </div>
                  <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                    <span>{text('COMMUNITY_LIKES_COLUMN_LABEL', '좋아요')}</span>
                    <button
                      type="button"
                      className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${(sortKey === 'likes' || sortKey === 'likesAsc') ? 'is-active' : ''}`.trim()}
                      aria-label={
                        sortKey === 'likesAsc'
                          ? text('COMMUNITY_LIKES_SORT_ASC_LABEL', '좋아요 오름차순 정렬')
                          : sortKey === 'likes'
                            ? text('COMMUNITY_LIKES_SORT_DESC_LABEL', '좋아요 내림차순 정렬')
                            : text('COMMUNITY_LIKES_SORT_DEFAULT_LABEL', '좋아요 기본 정렬')
                      }
                      onClick={() => toggleMetricSort('likes', 'likesAsc')}
                    >
                      <SortIcon direction={sortKey === 'likesAsc' ? 'asc' : sortKey === 'likes' ? 'desc' : 'none'} />
                    </button>
                  </div>
                  <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                    <span>{text('COMMUNITY_COMMENTS_COLUMN_LABEL', '댓글')}</span>
                    <button
                      type="button"
                      className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${(sortKey === 'comments' || sortKey === 'commentsAsc') ? 'is-active' : ''}`.trim()}
                      aria-label={
                        sortKey === 'commentsAsc'
                          ? text('COMMUNITY_COMMENTS_SORT_ASC_LABEL', '댓글 수 오름차순 정렬')
                          : sortKey === 'comments'
                            ? text('COMMUNITY_COMMENTS_SORT_DESC_LABEL', '댓글 수 내림차순 정렬')
                            : text('COMMUNITY_COMMENTS_SORT_DEFAULT_LABEL', '댓글 수 기본 정렬')
                      }
                      onClick={() => toggleMetricSort('comments', 'commentsAsc')}
                    >
                      <SortIcon direction={sortKey === 'commentsAsc' ? 'asc' : sortKey === 'comments' ? 'desc' : 'none'} />
                    </button>
                  </div>
                </div>

                {postPage.posts.length === 0 ? (
                  <div className="submit-history-row submit-history-empty-row community-empty-row data-table-empty-row" role="row">
                    <span className="submit-history-empty-cell data-table-empty-cell" role="cell" aria-live="polite">{text('COMMUNITY_EMPTY_STATE', '조건에 맞는 게시글이 없습니다.')}</span>
                  </div>
                ) : (
                  postPage.posts.map((post) => (
                    <article
                      key={post.id}
                      className="submit-history-row submit-history-body community-table-row"
                      role="row"
                      tabIndex={0}
                      aria-label={text('COMMUNITY_POST_OPEN_LABEL', { title: post.title }, `${post.title} 상세보기`)}
                      onClick={() => handleOpenPost(post.id)}
                      onKeyDown={(event) => handlePostRowKeyDown(event, post.id)}
                    >
                      <div role="cell" className="submit-history-cell community-table-cell community-table-category-cell" data-label={text('COMMUNITY_CATEGORY_COLUMN_LABEL', '구분')}>
                        <span className={`community-category-text is-${post.category}`}>{getCategoryLabel(post.category)}</span>
                      </div>

                      <div role="cell" className="submit-history-cell community-table-cell community-table-title-cell" data-label={text('COMMUNITY_TITLE_COLUMN_LABEL', '제목')}>
                        <button
                          type="button"
                          className="community-post-title-link"
                          onClick={(event) => {
                            event.stopPropagation();
                            handleOpenPost(post.id);
                          }}
                        >
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

                      <div role="cell" className="submit-history-cell community-table-cell community-table-handle-cell" data-label={text('COMMON_HANDLE_LABEL', 'Handle')}>
                        <button
                          type="button"
                          className="community-handle-link"
                          onClick={(event) => {
                            event.stopPropagation();
                            navigate(getProfilePath(post.authorHandle));
                          }}
                        >
                          {post.authorHandle}
                        </button>
                      </div>

                      <div role="cell" className="submit-history-cell community-table-cell community-table-date-cell" data-label={text('COMMUNITY_DATE_COLUMN_LABEL', '작성일')}>
                        {formatBoardDate(post.createdAt)}
                      </div>

                      <div role="cell" className="submit-history-cell community-table-cell community-table-views-cell" data-label={text('COMMUNITY_VIEWS_COLUMN_LABEL', '조회수')}>
                        {formatInteger(post.views)}
                      </div>

                      <div role="cell" className="submit-history-cell community-table-cell community-table-likes-cell" data-label={text('COMMUNITY_LIKES_COLUMN_LABEL', '좋아요')}>
                        {formatInteger(post.likes)}
                      </div>

                      <div role="cell" className="submit-history-cell community-table-cell community-table-comments-cell" data-label={text('COMMUNITY_COMMENTS_COLUMN_LABEL', '댓글')}>
                        {formatInteger(post.comments)}
                      </div>

                      <div role="cell" className="submit-history-cell community-table-cell community-table-mobile-meta-cell" data-label={text('COMMUNITY_MOBILE_META_LABEL', '게시글 정보')}>
                        <span className="community-mobile-meta-author">{post.authorHandle}</span>
                        <span>{formatBoardDate(post.createdAt)}</span>
                        <span className="community-mobile-metric" aria-label={text('COMMUNITY_VIEW_COUNT_LABEL', { count: formatInteger(post.views) }, `조회수 ${formatInteger(post.views)}`)}>
                          <ViewIcon />
                          <span>{formatInteger(post.views)}</span>
                        </span>
                        <span className="community-mobile-metric" aria-label={text('COMMUNITY_LIKE_COUNT_LABEL', { count: formatInteger(post.likes) }, `좋아요 ${formatInteger(post.likes)}`)}>
                          <LikeIcon />
                          <span>{formatInteger(post.likes)}</span>
                        </span>
                        <span className="community-mobile-metric" aria-label={text('COMMUNITY_COMMENT_COUNT_LABEL', { count: formatInteger(post.comments) }, `댓글 ${formatInteger(post.comments)}`)}>
                          <CommentIcon />
                          <span>{formatInteger(post.comments)}</span>
                        </span>
                      </div>
                    </article>
                  ))
                )}
              </DataTable>
            </div>
          </div>
        )}

        {!isLoading && !loadError.failed && postPage.totalCount > 0 ? (
          <Pagination
            className="problem-pagination submit-history-pagination"
            currentPage={postPage.currentPage}
            totalPages={postPage.totalPages}
            onPageChange={(page) => moveList(searchQuery, selectedCategory, sortKey, page)}
            ariaLabel={text('COMMUNITY_PAGE_LABEL', '커뮤니티 페이지')}
            inputLabel={text('COMMUNITY_PAGE_INPUT_LABEL', '이동할 커뮤니티 페이지 입력')}
            inputOpenLabel={text('COMMUNITY_PAGE_INPUT_OPEN_LABEL', '이동할 커뮤니티 페이지 입력 열기')}
            previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
            nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
          />
        ) : null}
        </section>
      )}
    </div>
  );
}
