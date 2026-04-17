import { useEffect, useMemo, useState } from 'react';
import CommunityPostList from '../components/community/CommunityPostList';
import { fetchCommunityPosts, type CommunityPostPage } from '../lib/communityApi';
import { COMMUNITY_PATH, COMMUNITY_WRITE_PATH, getCommunityPostPath, navigate } from '../lib/navigation';
import './CommunityPage.css';

type CommunitySortKey = 'relevance' | 'latest' | 'oldest' | 'views' | 'likes' | 'comments';

const PAGE_SIZE = 10;
const PAGE_NUMBER_GROUP_SIZE = 10;
const numberFormatter = new Intl.NumberFormat('ko-KR');

const sortOptions: Array<{ id: CommunitySortKey; label: string }> = [
  { id: 'relevance', label: '관련도순' },
  { id: 'latest', label: '최신순' },
  { id: 'oldest', label: '오래된순' },
  { id: 'views', label: '조회수순' },
  { id: 'likes', label: '좋아요순' },
  { id: 'comments', label: '댓글많은순' },
];

function isSortKey(value: string | null): value is CommunitySortKey {
  return sortOptions.some((option) => option.id === value);
}

function readCommunityListState() {
  const params = new URLSearchParams(window.location.search);
  const search = params.get('search') ?? '';
  const sort = params.get('sort');
  const page = Number.parseInt(params.get('page') ?? '1', 10);
  const tag = params.get('tag') ?? '';

  return {
    search,
    tag,
    sortKey: isSortKey(sort) ? sort : 'latest',
    page: Number.isNaN(page) || page < 1 ? 1 : page,
  };
}

function buildCommunityListPath({
  search,
  tag,
  sortKey,
  page,
}: {
  search: string;
  tag: string;
  sortKey: CommunitySortKey;
  page: number;
}) {
  const params = new URLSearchParams();
  const normalizedSearch = search.trim();
  const normalizedTag = tag.trim();

  if (normalizedSearch) {
    params.set('search', normalizedSearch);
  }

  if (normalizedTag) {
    params.set('tag', normalizedTag);
  }

  if (sortKey !== 'latest') {
    params.set('sort', sortKey);
  }

  if (page > 1) {
    params.set('page', String(page));
  }

  const query = params.toString();
  return query ? `${COMMUNITY_PATH}?${query}` : COMMUNITY_PATH;
}

function formatNumber(value: number) {
  return numberFormatter.format(value);
}

const emptyPage: CommunityPostPage = {
  currentPage: 1,
  pageSize: PAGE_SIZE,
  totalCount: 0,
  totalPages: 1,
  posts: [],
};

export default function CommunityPage() {
  const initialState = readCommunityListState();
  const [draftSearchValue, setDraftSearchValue] = useState(initialState.search);
  const [searchQuery, setSearchQuery] = useState(initialState.search);
  const [activeTag, setActiveTag] = useState(initialState.tag);
  const [sortKey, setSortKey] = useState<CommunitySortKey>(initialState.sortKey);
  const [requestedPage, setRequestedPage] = useState(initialState.page);
  const [postPage, setPostPage] = useState<CommunityPostPage>(emptyPage);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const savedScrollY = window.history.state?.scrollY;

    if (typeof savedScrollY === 'number') {
      window.requestAnimationFrame(() => {
        window.scrollTo({ top: savedScrollY, behavior: 'auto' });
      });
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    setIsLoading(true);
    setErrorMessage(null);

    fetchCommunityPosts({
      page: requestedPage,
      search: searchQuery,
      tag: activeTag,
      sortKey,
    })
      .then((nextPostPage) => {
        if (cancelled) {
          return;
        }

        setPostPage(nextPostPage);
        if (requestedPage !== nextPostPage.currentPage) {
          setRequestedPage(nextPostPage.currentPage);
          replaceListHistory(searchQuery, activeTag, sortKey, nextPostPage.currentPage, 0);
        }
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }

        setPostPage(emptyPage);
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
  }, [activeTag, requestedPage, searchQuery, sortKey]);

  const currentPage = postPage.currentPage;
  const totalPages = postPage.totalPages;
  const pageGroupStart = Math.floor((currentPage - 1) / PAGE_NUMBER_GROUP_SIZE) * PAGE_NUMBER_GROUP_SIZE + 1;
  const pageGroupEnd = Math.min(totalPages, pageGroupStart + PAGE_NUMBER_GROUP_SIZE - 1);
  const hasSearchQuery = useMemo(() => searchQuery.trim().length > 0, [searchQuery]);

  function replaceListHistory(nextSearch: string, nextTag: string, nextSortKey: CommunitySortKey, nextPage: number, scrollY = 0) {
    window.history.replaceState(
      { ...(window.history.state ?? {}), scrollY },
      '',
      buildCommunityListPath({ search: nextSearch, tag: nextTag, sortKey: nextSortKey, page: nextPage }),
    );
  }

  function moveList(nextSearch: string, nextTag: string, nextSortKey: CommunitySortKey, nextPage: number) {
    const trimmedSearch = nextSearch.trim();
    const normalizedTag = nextTag.trim();

    setSearchQuery(trimmedSearch);
    setActiveTag(normalizedTag);
    setSortKey(nextSortKey);
    setRequestedPage(nextPage);
    replaceListHistory(trimmedSearch, normalizedTag, nextSortKey, nextPage, 0);
    window.scrollTo({ top: 0, behavior: 'auto' });
  }

  function applySearch(value: string) {
    const trimmedValue = value.trim();
    const nextSortKey = trimmedValue && sortKey === 'latest' ? 'relevance' : trimmedValue ? sortKey : sortKey === 'relevance' ? 'latest' : sortKey;

    setDraftSearchValue(value);
    moveList(trimmedValue, activeTag, nextSortKey, 1);
  }

  function handleOpenPost(postId: string) {
    const fromPath = buildCommunityListPath({ search: searchQuery, tag: activeTag, sortKey, page: currentPage });

    window.history.replaceState({ ...(window.history.state ?? {}), scrollY: window.scrollY }, '', fromPath);
    navigate(getCommunityPostPath(postId), {
      state: {
        from: fromPath,
      },
    });
    window.scrollTo({ top: 0, behavior: 'auto' });
  }

  function handleSelectTag(tag: string) {
    moveList(searchQuery, tag, sortKey === 'relevance' && !hasSearchQuery ? 'latest' : sortKey, 1);
  }

  function handleResetFilters() {
    setDraftSearchValue('');
    moveList('', '', 'latest', 1);
  }

  return (
    <div className="page-stack community-page">
      <section className="panel-card compact community-search-panel">
        <form
          className="community-search-form"
          onSubmit={(event) => {
            event.preventDefault();
            applySearch(draftSearchValue);
          }}
        >
          <label className="problem-search-field community-search-field">
            <span className="problem-search-icon" aria-hidden="true">
              ⌕
            </span>
            <input
              type="search"
              value={draftSearchValue}
              onChange={(event) => setDraftSearchValue(event.target.value)}
              className="text-field problem-search-input community-search-input"
              placeholder="제목, 작성자, 태그, 내용 검색"
              aria-label="커뮤니티 게시글 검색"
            />
          </label>

          <button type="submit" className="btn secondary problem-search-button community-search-button">
            검색
          </button>
        </form>

        {activeTag ? (
          <div className="community-active-filter-row">
            <button type="button" className="community-active-tag-chip" onClick={() => moveList(searchQuery, '', sortKey, 1)}>
              <span>#{activeTag}</span>
              <span aria-hidden="true">x</span>
            </button>
            <span className="community-active-filter-caption">태그 기준으로 목록을 보고 있다.</span>
          </div>
        ) : null}
      </section>

      <section className="panel-card compact community-list-panel">
        <div className="community-list-toolbar">
          <div className="community-list-meta">
            <span className="community-result-count">총 {formatNumber(postPage.totalCount)}개</span>

            <div className="community-sort-box">
              <label className="community-sort-label" htmlFor="community-sort-select">
                정렬
              </label>
              <div className="community-sort-select-wrap">
                <select
                  id="community-sort-select"
                  className="community-sort-select"
                  value={sortKey}
                  onChange={(event) => moveList(searchQuery, activeTag, event.target.value as CommunitySortKey, 1)}
                >
                  {sortOptions.map((option) => (
                    <option key={option.id} value={option.id}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          <button
            type="button"
            className="btn primary community-write-button"
            onClick={() =>
              navigate(COMMUNITY_WRITE_PATH, {
                state: {
                  from: buildCommunityListPath({ search: searchQuery, tag: activeTag, sortKey, page: currentPage }),
                },
              })
            }
          >
            글쓰기
          </button>
        </div>

        {isLoading ? <div className="community-empty-state">데이터 로딩중</div> : null}
        {!isLoading && errorMessage ? <div className="community-empty-state">{errorMessage}</div> : null}
        {!isLoading && !errorMessage ? (
          <CommunityPostList
            posts={postPage.posts}
            searchQuery={searchQuery}
            activeTag={activeTag}
            onOpenPost={handleOpenPost}
            onSelectTag={handleSelectTag}
            onResetFilters={handleResetFilters}
          />
        ) : null}

        {!isLoading && !errorMessage && postPage.totalCount > 0 ? (
          <div className="problem-pagination community-pagination" role="navigation" aria-label="커뮤니티 페이지">
            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => moveList(searchQuery, activeTag, sortKey, Math.max(1, currentPage - 1))}
              disabled={currentPage === 1}
            >
              이전
            </button>

            <div className="problem-page-numbers">
              {Array.from({ length: pageGroupEnd - pageGroupStart + 1 }, (_, index) => pageGroupStart + index).map((page) => (
                <button
                  key={page}
                  type="button"
                  className={`mini-toggle problem-page-button ${page === currentPage ? 'is-selected' : ''}`}
                  aria-current={page === currentPage ? 'page' : undefined}
                  onClick={() => moveList(searchQuery, activeTag, sortKey, page)}
                >
                  {page}
                </button>
              ))}
            </div>

            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => moveList(searchQuery, activeTag, sortKey, Math.min(totalPages, currentPage + 1))}
              disabled={currentPage === totalPages}
            >
              다음
            </button>

            <span className="problem-pagination-meta">
              {currentPage} / {totalPages}
            </span>
          </div>
        ) : null}
      </section>
    </div>
  );
}
