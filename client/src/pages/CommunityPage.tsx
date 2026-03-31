import { useEffect, useState, useSyncExternalStore } from 'react';
import CommunityPostList from '../components/community/CommunityPostList';
import {
  getCommunityPosts,
  getCommunityStoreSnapshot,
  subscribeCommunityStore,
} from '../lib/communityStore';
import { COMMUNITY_PATH, COMMUNITY_WRITE_PATH, getCommunityPostPath, navigate } from '../lib/navigation';
import { mockCommunityTagLibrary } from '../mocks/community';
import type { CommunityPostSummary } from '../types/domain';

type CommunitySortKey = 'relevance' | 'latest' | 'oldest' | 'views' | 'likes' | 'comments';
type RankedCommunityPost = {
  post: CommunityPostSummary;
  relevanceScore: number;
};

const PAGE_SIZE = 10;
const PAGE_NUMBER_GROUP_SIZE = 10;
const numberFormatter = new Intl.NumberFormat('ko-KR');
const tagLibraryByLabel = new Map(mockCommunityTagLibrary.map((tag) => [tag.label, tag]));

const sortOptions: Array<{ id: CommunitySortKey; label: string }> = [
  { id: 'relevance', label: '관련도순' },
  { id: 'latest', label: '최신순' },
  { id: 'oldest', label: '과거순' },
  { id: 'views', label: '조회수순' },
  { id: 'likes', label: '좋아요순' },
  { id: 'comments', label: '댓글순' },
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

function normalizeKeyword(value: string) {
  return value
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[_\-\s]+/g, '')
    .replace(/[^\p{L}\p{N}]/gu, '');
}

function tokenize(value: string) {
  return value
    .split(/\s+/)
    .map((token) => normalizeKeyword(token))
    .filter(Boolean);
}

function resolveTagLabel(value: string) {
  const normalizedValue = normalizeKeyword(value);
  const matchedTag = mockCommunityTagLibrary.find((tag) =>
    [tag.label, ...tag.aliases].some((candidate) => normalizeKeyword(candidate) === normalizedValue)
  );

  return matchedTag?.label ?? value.trim();
}

function scoreMatch(
  candidates: string[],
  wholeQuery: string,
  tokens: string[],
  weights: { exact: number; prefix: number; partial: number; token: number }
) {
  let bestScore = 0;

  for (const candidate of candidates) {
    const normalizedCandidate = normalizeKeyword(candidate);
    let score = 0;

    if (wholeQuery) {
      if (normalizedCandidate === wholeQuery) {
        score += weights.exact;
      } else if (normalizedCandidate.startsWith(wholeQuery)) {
        score += weights.prefix;
      } else if (normalizedCandidate.includes(wholeQuery)) {
        score += weights.partial;
      }
    }

    for (const token of tokens) {
      if (normalizedCandidate.includes(token)) {
        score += weights.token;
      }
    }

    bestScore = Math.max(bestScore, score);
  }

  return bestScore;
}

function getTagCandidates(tagLabels: string[]) {
  return tagLabels.flatMap((label) => {
    const definition = tagLibraryByLabel.get(label);
    return definition ? [definition.label, ...definition.aliases] : [label];
  });
}

function getPostSearchScore(post: CommunityPostSummary, wholeQuery: string, tokens: string[]) {
  if (!wholeQuery && tokens.length === 0) {
    return 0;
  }

  const titleScore = scoreMatch([post.title], wholeQuery, tokens, {
    exact: 180,
    prefix: 138,
    partial: 110,
    token: 32,
  });
  const authorScore = scoreMatch([post.authorHandle], wholeQuery, tokens, {
    exact: 144,
    prefix: 116,
    partial: 88,
    token: 26,
  });
  const tagScore = scoreMatch(getTagCandidates(post.tags), wholeQuery, tokens, {
    exact: 168,
    prefix: 132,
    partial: 100,
    token: 30,
  });
  const excerptScore = scoreMatch([post.excerpt], wholeQuery, tokens, {
    exact: 96,
    prefix: 82,
    partial: 60,
    token: 14,
  });
  const contentScore = scoreMatch([post.content], wholeQuery, tokens, {
    exact: 84,
    prefix: 70,
    partial: 52,
    token: 12,
  });
  const searchBlob = normalizeKeyword([post.title, post.authorHandle, post.excerpt, post.content, ...post.tags].join(' '));
  const coverageBonus = tokens.length > 1 && tokens.every((token) => searchBlob.includes(token)) ? 24 : 0;

  return titleScore + authorScore + tagScore + excerptScore + contentScore + coverageBonus;
}

function compareRankedPosts(sortKey: CommunitySortKey, hasSearchQuery: boolean, left: RankedCommunityPost, right: RankedCommunityPost) {
  const pinnedGap = Number(right.post.isPinned) - Number(left.post.isPinned);
  if (pinnedGap !== 0) {
    return pinnedGap;
  }

  const relevanceGap = right.relevanceScore - left.relevanceScore;
  const latestGap = Date.parse(right.post.createdAt) - Date.parse(left.post.createdAt);
  const oldestGap = Date.parse(left.post.createdAt) - Date.parse(right.post.createdAt);
  const viewsGap = right.post.views - left.post.views;
  const likesGap = right.post.likes - left.post.likes;
  const commentsGap = right.post.comments - left.post.comments;

  if (sortKey === 'relevance') {
    if (hasSearchQuery && relevanceGap !== 0) {
      return relevanceGap;
    }

    return latestGap || viewsGap || likesGap || commentsGap || left.post.title.localeCompare(right.post.title, 'ko-KR');
  }

  if (sortKey === 'oldest') {
    return oldestGap || relevanceGap || viewsGap || likesGap || commentsGap || left.post.title.localeCompare(right.post.title, 'ko-KR');
  }

  if (sortKey === 'views') {
    return viewsGap || relevanceGap || latestGap || likesGap || commentsGap || left.post.title.localeCompare(right.post.title, 'ko-KR');
  }

  if (sortKey === 'likes') {
    return likesGap || relevanceGap || latestGap || viewsGap || commentsGap || left.post.title.localeCompare(right.post.title, 'ko-KR');
  }

  if (sortKey === 'comments') {
    return commentsGap || relevanceGap || latestGap || viewsGap || likesGap || left.post.title.localeCompare(right.post.title, 'ko-KR');
  }

  return latestGap || relevanceGap || viewsGap || likesGap || commentsGap || left.post.title.localeCompare(right.post.title, 'ko-KR');
}

function matchesActiveTag(post: CommunityPostSummary, activeTag: string) {
  if (!activeTag.trim()) {
    return true;
  }

  const normalizedActiveTag = normalizeKeyword(activeTag);
  return getTagCandidates(post.tags).some((candidate) => normalizeKeyword(candidate) === normalizedActiveTag);
}

function formatNumber(value: number) {
  return numberFormatter.format(value);
}

export default function CommunityPage() {
  useSyncExternalStore(subscribeCommunityStore, getCommunityStoreSnapshot, () => '');

  const initialState = readCommunityListState();
  const [draftSearchValue, setDraftSearchValue] = useState(initialState.search);
  const [searchQuery, setSearchQuery] = useState(initialState.search);
  const [activeTag, setActiveTag] = useState(initialState.tag);
  const [sortKey, setSortKey] = useState<CommunitySortKey>(initialState.sortKey);
  const [requestedPage, setRequestedPage] = useState(initialState.page);

  useEffect(() => {
    const savedScrollY = window.history.state?.scrollY;

    if (typeof savedScrollY === 'number') {
      window.requestAnimationFrame(() => {
        window.scrollTo({ top: savedScrollY, behavior: 'auto' });
      });
    }
  }, []);

  const normalizedSearchQuery = normalizeKeyword(searchQuery);
  const searchTokens = tokenize(searchQuery);
  const hasSearchQuery = normalizedSearchQuery.length > 0 || searchTokens.length > 0;
  const canonicalTag = activeTag ? resolveTagLabel(activeTag) : '';
  const posts = getCommunityPosts();

  const filteredPosts = posts
    .filter((post) => matchesActiveTag(post, canonicalTag))
    .map((post) => ({
      post,
      relevanceScore: getPostSearchScore(post, normalizedSearchQuery, searchTokens),
    }))
    .filter(({ relevanceScore }) => !hasSearchQuery || relevanceScore > 0)
    .sort((left, right) => compareRankedPosts(sortKey, hasSearchQuery, left, right));

  const totalPages = Math.max(1, Math.ceil(filteredPosts.length / PAGE_SIZE));
  const currentPage = Math.min(requestedPage, totalPages);
  const pageGroupStart = Math.floor((currentPage - 1) / PAGE_NUMBER_GROUP_SIZE) * PAGE_NUMBER_GROUP_SIZE + 1;
  const pageGroupEnd = Math.min(totalPages, pageGroupStart + PAGE_NUMBER_GROUP_SIZE - 1);
  const pagedPosts = filteredPosts.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE).map(({ post }) => post);

  useEffect(() => {
    if (requestedPage !== currentPage) {
      setRequestedPage(currentPage);
      window.history.replaceState(
        { ...(window.history.state ?? {}), scrollY: 0 },
        '',
        buildCommunityListPath({ search: searchQuery, tag: canonicalTag, sortKey, page: currentPage })
      );
    }
  }, [canonicalTag, currentPage, requestedPage, searchQuery, sortKey]);

  function replaceListHistory(nextSearch: string, nextTag: string, nextSortKey: CommunitySortKey, nextPage: number, scrollY = 0) {
    window.history.replaceState(
      { ...(window.history.state ?? {}), scrollY },
      '',
      buildCommunityListPath({ search: nextSearch, tag: nextTag, sortKey: nextSortKey, page: nextPage })
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
    const nextSortKey = trimmedValue ? 'relevance' : sortKey === 'relevance' ? 'latest' : sortKey;

    setDraftSearchValue(value);
    moveList(trimmedValue, canonicalTag, nextSortKey, 1);
  }

  function handleOpenPost(postId: string) {
    const fromPath = buildCommunityListPath({ search: searchQuery, tag: canonicalTag, sortKey, page: currentPage });

    window.history.replaceState({ ...(window.history.state ?? {}), scrollY: window.scrollY }, '', fromPath);
    navigate(getCommunityPostPath(postId), {
      state: {
        from: fromPath,
      },
    });
    window.scrollTo({ top: 0, behavior: 'auto' });
  }

  function handleSelectTag(tag: string) {
    moveList(searchQuery, tag, sortKey === 'relevance' && !searchQuery.trim() ? 'latest' : sortKey, 1);
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
              placeholder="제목, 아이디, 태그, 내용 검색"
              aria-label="커뮤니티 게시글 검색"
            />
          </label>

          <button type="submit" className="btn secondary problem-search-button community-search-button">
            검색
          </button>
        </form>

        {canonicalTag ? (
          <div className="community-active-filter-row">
            <button type="button" className="community-active-tag-chip" onClick={() => moveList(searchQuery, '', sortKey, 1)}>
              <span>#{canonicalTag}</span>
              <span aria-hidden="true">x</span>
            </button>
            <span className="community-active-filter-caption">태그별 목록을 보고 있습니다.</span>
          </div>
        ) : null}
      </section>

      <section className="panel-card compact community-list-panel">
        <div className="community-list-toolbar">
          <div className="community-list-meta">
            <span className="community-result-count">총 {formatNumber(filteredPosts.length)}개</span>

            <div className="community-sort-box">
              <label className="community-sort-label" htmlFor="community-sort-select">
                정렬
              </label>
              <div className="community-sort-select-wrap">
                <select
                  id="community-sort-select"
                  className="community-sort-select"
                  value={sortKey}
                  onChange={(event) => {
                    moveList(searchQuery, canonicalTag, event.target.value as CommunitySortKey, 1);
                  }}
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
                  from: buildCommunityListPath({ search: searchQuery, tag: canonicalTag, sortKey, page: currentPage }),
                },
              })
            }
          >
            글쓰기
          </button>
        </div>

        <CommunityPostList
          posts={pagedPosts}
          searchQuery={searchQuery}
          activeTag={canonicalTag}
          onOpenPost={handleOpenPost}
          onSelectTag={handleSelectTag}
          onResetFilters={handleResetFilters}
        />

        {filteredPosts.length > 0 ? (
          <div className="problem-pagination community-pagination" role="navigation" aria-label="커뮤니티 페이지">
            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => moveList(searchQuery, canonicalTag, sortKey, Math.max(1, currentPage - 1))}
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
                  onClick={() => moveList(searchQuery, canonicalTag, sortKey, page)}
                >
                  {page}
                </button>
              ))}
            </div>

            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => moveList(searchQuery, canonicalTag, sortKey, Math.min(totalPages, currentPage + 1))}
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
