import { useEffect, useMemo, useRef, useState } from 'react';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import { DataTable } from '../components/common/DataTable';
import ContentLoading from '../components/common/LoadingSpinner';
import PageErrorState from '../components/common/PageErrorState';
import Pagination from '../components/common/Pagination';
import { SearchForm } from '../components/common/PageToolbar';
import SortIcon from '../components/icons/SortIcon';
import { useLocationSearch } from '../hooks/useLocationState';
import useRequestState from '../hooks/useRequestState';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '../lib/favoriteTabs';
import CommunityWritePage from './CommunityWritePage';
import { fetchCommunityPosts, type CommunityPostPage } from '../lib/communityApi';
import { COMMUNITY_PATH, COMMUNITY_WRITE_PATH, getCommunityPostPath, getProfilePath, navigate } from '../lib/navigation';
import { useMockSession } from '../lib/session';
import { getUiTextValue, useUiText } from '../lib/uiText';
import { formatBoardDate, formatInteger } from '../lib/formatters';
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

const emptyPage: CommunityPostPage = {
  currentPage: 1,
  pageSize: PAGE_SIZE,
  totalCount: 0,
  totalPages: 1,
  posts: [],
};

export default function CommunityPage() {
  const { text } = useUiText();
  const { isAuthenticated } = useMockSession();
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

  return (
    <div className="page page-stack home-page community-page">
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
          ) : null}
        </div>
      </section>

      {isWriteMode ? (
        <section className="panel-card problem-board community-board community-write-board data-board">
          <CommunityWritePage embedded />
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
                  <div className="submit-history-row submit-history-empty-row community-empty-row" role="row">
                    <span className="submit-history-empty-cell" role="cell">{text('COMMUNITY_EMPTY_STATE', '조건에 맞는 게시글이 없습니다.')}</span>
                  </div>
                ) : (
                  postPage.posts.map((post) => (
                    <article key={post.id} className="submit-history-row submit-history-body community-table-row" role="row">
                      <div role="cell" className="submit-history-cell community-table-cell" data-label={text('COMMUNITY_CATEGORY_COLUMN_LABEL', '구분')}>
                        <span className={`community-category-text is-${post.category}`}>{getCategoryLabel(post.category)}</span>
                      </div>

                      <div role="cell" className="submit-history-cell community-table-cell community-table-title-cell" data-label={text('COMMUNITY_TITLE_COLUMN_LABEL', '제목')}>
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

                      <div role="cell" className="submit-history-cell community-table-cell" data-label={text('COMMON_HANDLE_LABEL', 'Handle')}>
                        <button
                          type="button"
                          className="community-handle-link"
                          onClick={() => navigate(getProfilePath(post.authorHandle))}
                        >
                          {post.authorHandle}
                        </button>
                      </div>

                      <div role="cell" className="submit-history-cell community-table-cell community-table-date-cell" data-label={text('COMMUNITY_DATE_COLUMN_LABEL', '작성일')}>
                        {formatBoardDate(post.updatedAt ?? post.createdAt)}
                      </div>

                      <div role="cell" className="submit-history-cell community-table-cell" data-label={text('COMMUNITY_VIEWS_COLUMN_LABEL', '조회수')}>
                        {formatInteger(post.views)}
                      </div>

                      <div role="cell" className="submit-history-cell community-table-cell" data-label={text('COMMUNITY_LIKES_COLUMN_LABEL', '좋아요')}>
                        {formatInteger(post.likes)}
                      </div>

                      <div role="cell" className="submit-history-cell community-table-cell" data-label={text('COMMUNITY_COMMENTS_COLUMN_LABEL', '댓글')}>
                        {formatInteger(post.comments)}
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
