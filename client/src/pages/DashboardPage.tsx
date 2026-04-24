import { useEffect, useMemo, useState } from 'react';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import {
  fetchDashboard,
  type DashboardCommunityPost,
  type DashboardData,
  type DashboardProblemRecommendation,
} from '../lib/dashboardApi';
import {
  COMMUNITY_PATH,
  PROBLEMS_PATH,
  getCommunityPostPath,
  getProfilePath,
  navigate,
} from '../lib/navigation';
import { useHomeSiteTitle } from '../lib/uiText';
import type { DbmsType } from '../types/domain';
import './DashboardPage.css';

const compactNumberFormatter = new Intl.NumberFormat('ko-KR', {
  notation: 'compact',
  maximumFractionDigits: 1,
});
const percentFormatter = new Intl.NumberFormat('ko-KR', {
  maximumFractionDigits: 1,
});
const COMMUNITY_FEATURED_PAGE_SIZE = 4;
const COMMUNITY_GRID_PAGE_SIZE = 9;
const PROBLEM_PAGE_SIZE = 6;
const MAX_PROBLEM_RECOMMENDATION_COUNT = 9;

function createEmptyDashboard(): DashboardData {
  return {
    authenticated: false,
    currentHandle: null,
    communityPosts: [],
    problems: [],
  };
}

function formatDbmsLabel(dbms: DbmsType) {
  return dbms === 'oracle' ? 'Oracle' : 'PostgreSQL';
}

function formatCount(value: number) {
  return compactNumberFormatter.format(value);
}

function getCategoryLabel(category: string) {
  if (category === 'notice') {
    return '공지';
  }

  if (category === 'question') {
    return '질문';
  }

  return '자유';
}

function buildProblemPath(problemId: string) {
  return `${PROBLEMS_PATH}/${encodeURIComponent(problemId)}`;
}

function DashboardLoadingOverlay() {
  return <div className="dashboard-loading-overlay" aria-hidden="true" />;
}

function DashboardCommunityLoadingCards({
  cardCount,
  featuredLayout,
}: {
  cardCount: number;
  featuredLayout: boolean;
}) {
  return Array.from({ length: cardCount }, (_, index) => (
    <article
      key={`community-loading-${index}`}
      className={`dashboard-post-card dashboard-loading-card ${featuredLayout && index === 0 ? 'is-featured' : 'is-compact'}`.trim()}
      aria-hidden="true"
    >
      <div className="dashboard-post-topline dashboard-loading-headline">
        <span className="dashboard-loading-chip" />
      </div>

      <span className="dashboard-loading-line is-title" />

      <div className="dashboard-post-tags" aria-hidden="true">
        <span className="dashboard-loading-tag" />
        <span className="dashboard-loading-tag" />
      </div>

      {featuredLayout && index === 0 ? (
        <div className="dashboard-loading-copy">
          <span className="dashboard-loading-line is-copy" />
          <span className="dashboard-loading-line is-copy is-wide" />
          <span className="dashboard-loading-line is-copy is-short" />
        </div>
      ) : null}

      <div className="dashboard-post-footer dashboard-loading-footer">
        <span className="dashboard-loading-line is-handle" />
        <div className="dashboard-post-metrics dashboard-loading-metrics">
          <span className="dashboard-loading-dot" />
          <span className="dashboard-loading-dot" />
          <span className="dashboard-loading-dot" />
        </div>
      </div>
    </article>
  ));
}

function DashboardProblemLoadingRows() {
  return Array.from({ length: PROBLEM_PAGE_SIZE }, (_, index) => (
    <article key={`problem-loading-${index}`} className="dashboard-problem-card dashboard-loading-row" aria-hidden="true">
      <div className="dashboard-problem-card-head">
        <span className="dashboard-loading-chip is-problem" />
        <span className="dashboard-loading-line is-problem-title" />
      </div>

      <div className="dashboard-problem-meta dashboard-loading-problem-meta">
        <span className="dashboard-loading-metric" />
        <span className="dashboard-loading-metric" />
        <span className="dashboard-loading-metric" />
        <span className="dashboard-loading-metric" />
      </div>
    </article>
  ));
}

function CommunitySectionIcon() {
  return (
    <svg viewBox="0 0 22 22" fill="none" aria-hidden="true">
      <path d="m11 2.8 2.08 4.2 4.64.68-3.36 3.26.8 4.62L11 13.38l-4.16 2.18.8-4.62-3.36-3.26L8.92 7 11 2.8Z" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ProblemSectionIcon() {
  return (
    <svg viewBox="0 0 22 22" fill="none" aria-hidden="true">
      <path d="M7.2 4.2h7.6M6 7.5h10M6 10.8h6.2M4.8 2.9h12.4v16.2H4.8V2.9Z" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function SectionLinkIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="m6 3.5 4.5 4.5L6 12.5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ViewIcon() {
  return (
    <svg viewBox="0 0 18 18" fill="none" aria-hidden="true">
      <path d="M2.7 9s2.15-4.2 6.3-4.2S15.3 9 15.3 9 13.15 13.2 9 13.2 2.7 9 2.7 9Z" stroke="currentColor" strokeWidth="1.45" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M9 10.95A1.95 1.95 0 1 0 9 7.05a1.95 1.95 0 0 0 0 3.9Z" stroke="currentColor" strokeWidth="1.45" />
    </svg>
  );
}

function LikeIcon() {
  return (
    <svg viewBox="0 0 18 18" fill="none" aria-hidden="true">
      <path d="M9 14.65 4.1 10.1a3.05 3.05 0 0 1 4.35-4.32L9 6.32l.55-.54a3.05 3.05 0 0 1 4.35 4.32L9 14.65Z" stroke="currentColor" strokeWidth="1.45" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function CommentIcon() {
  return (
    <svg viewBox="0 0 18 18" fill="none" aria-hidden="true">
      <path d="M4.1 4.35h9.8v6.75H8.15l-3.25 2.55V11.1h-.8V4.35Z" stroke="currentColor" strokeWidth="1.45" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function CarouselArrowIcon({ direction }: { direction: 'previous' | 'next' }) {
  return (
    <svg viewBox="0 0 18 18" aria-hidden="true">
      {direction === 'previous' ? (
        <path d="M11.2 4.2 6.4 9l4.8 4.8" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
      ) : (
        <path d="M6.8 4.2 11.6 9l-4.8 4.8" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
      )}
    </svg>
  );
}

function CarouselDots({
  activePage,
  label,
  pageCount,
  onSelect,
}: {
  activePage: number;
  label: string;
  pageCount: number;
  onSelect: (pageIndex: number) => void;
}) {
  return (
    <div className="dashboard-carousel-dots" aria-label={label}>
      {Array.from({ length: pageCount }, (_, pageIndex) => (
        <button
          key={pageIndex}
          type="button"
          className={`dashboard-carousel-dot ${pageIndex === activePage ? 'is-active' : ''}`}
          onClick={() => onSelect(pageIndex)}
          aria-label={`${pageIndex + 1}페이지 보기`}
          aria-pressed={pageIndex === activePage}
        />
      ))}
    </div>
  );
}

function createCommunityPages(posts: DashboardCommunityPost[]) {
  const pages: DashboardCommunityPost[][] = [];
  const featuredPosts = posts.slice(0, COMMUNITY_FEATURED_PAGE_SIZE);

  if (featuredPosts.length > 0) {
    pages.push(featuredPosts);
  }

  for (let cursor = COMMUNITY_FEATURED_PAGE_SIZE; cursor < posts.length; cursor += COMMUNITY_GRID_PAGE_SIZE) {
    pages.push(posts.slice(cursor, cursor + COMMUNITY_GRID_PAGE_SIZE));
  }

  return pages;
}

function createProblemPages(problems: DashboardProblemRecommendation[]) {
  const recommendationProblems = problems.slice(0, MAX_PROBLEM_RECOMMENDATION_COUNT);
  const pageCount = Math.ceil(recommendationProblems.length / PROBLEM_PAGE_SIZE);

  return Array.from({ length: pageCount }, (_, pageIndex) => {
    const startIndex = pageIndex * PROBLEM_PAGE_SIZE;
    return recommendationProblems.slice(startIndex, startIndex + PROBLEM_PAGE_SIZE);
  });
}

function CommunityMetric({
  type,
  value,
}: {
  type: 'views' | 'likes' | 'comments';
  value: number;
}) {
  const label = type === 'views' ? '조회수' : type === 'likes' ? '좋아요' : '댓글';
  const icon = type === 'views' ? <ViewIcon /> : type === 'likes' ? <LikeIcon /> : <CommentIcon />;

  return (
    <span className={`dashboard-post-metric is-${type}`} aria-label={`${label} ${formatCount(value)}`}>
      {icon}
      <span>{formatCount(value)}</span>
    </span>
  );
}

function ProblemMeta({ problem }: { problem: DashboardProblemRecommendation }) {
  return (
    <div className="dashboard-problem-meta" aria-label={`${problem.problemId} 추천 지표`}>
      <span className="dashboard-problem-metric">
        <span>해결</span>
        <strong>{`${formatCount(problem.solvedUserCount)}명`}</strong>
      </span>
      <span className="dashboard-problem-metric">
        <span>제출</span>
        <strong>{`${formatCount(problem.totalSubmitCount)}회`}</strong>
      </span>
      <span className="dashboard-problem-metric">
        <span>정답</span>
        <strong>{`${formatCount(problem.successSubmitCount)}회`}</strong>
      </span>
      <span className="dashboard-problem-metric">
        <span>Cost 편차</span>
        <strong>{`${percentFormatter.format(problem.spreadRate)}%`}</strong>
      </span>
    </div>
  );
}

function CommunityPostCard({
  featured,
  post,
}: {
  featured: boolean;
  post: DashboardCommunityPost;
}) {
  return (
    <article className={`dashboard-post-card ${featured ? 'is-featured' : 'is-compact'}`.trim()}>
      <button
        type="button"
        className="dashboard-card-hitbox"
        onClick={() => navigate(getCommunityPostPath(post.postId))}
        aria-label={`${post.title} 게시글로 이동`}
      />

      <div className="dashboard-post-topline">
        <span className={`dashboard-post-category is-${post.category}`}>{getCategoryLabel(post.category)}</span>
        <h2 className="dashboard-post-title">{post.title}</h2>
      </div>

      {post.tags.length > 0 ? (
        <div className="dashboard-post-tags" aria-label="게시글 태그">
          {post.tags.slice(0, featured ? 4 : 3).map((tag) => (
            <span key={tag}>{`#${tag}`}</span>
          ))}
        </div>
      ) : null}

      {featured ? <p className="dashboard-post-excerpt">{post.excerpt || '본문 미리보기가 없습니다.'}</p> : null}

      <div className="dashboard-post-footer">
        <div className="dashboard-post-metrics" aria-label="게시글 반응">
          <CommunityMetric type="views" value={post.viewCount} />
          <CommunityMetric type="likes" value={post.likeCount} />
          <CommunityMetric type="comments" value={post.commentCount} />
        </div>

        <button
          type="button"
          className="dashboard-inline-link"
          onClick={(event) => {
            event.stopPropagation();
            navigate(getProfilePath(post.authorHandle));
          }}
        >
          {post.authorHandle}
        </button>
      </div>
    </article>
  );
}

export default function DashboardPage() {
  useHomeSiteTitle('Quertimizer Dashboard');
  const [dashboard, setDashboard] = useState<DashboardData>(createEmptyDashboard);
  const [isLoading, setIsLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [communityPageIndex, setCommunityPageIndex] = useState(0);
  const [problemPageIndex, setProblemPageIndex] = useState(0);
  const communityPages = useMemo(() => createCommunityPages(dashboard.communityPosts), [dashboard.communityPosts]);
  const problemPages = useMemo(() => createProblemPages(dashboard.problems), [dashboard.problems]);
  const communityPageCount = Math.max(1, communityPages.length);
  const problemPageCount = Math.max(1, problemPages.length);
  const visibleCommunityPosts = communityPages[communityPageIndex] ?? [];
  const visibleProblems = problemPages[problemPageIndex] ?? [];
  const isFeaturedCommunityLayout = communityPageIndex === 0;
  const communityLoadingCardCount = isFeaturedCommunityLayout ? COMMUNITY_FEATURED_PAGE_SIZE : COMMUNITY_GRID_PAGE_SIZE;

  useEffect(() => {
    document.documentElement.classList.add('is-dashboard-route');

    return () => {
      document.documentElement.classList.remove('is-dashboard-route');
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function loadDashboard() {
      setIsLoading(true);
      setLoadFailed(false);

      try {
        const fetchedDashboard = await fetchDashboard();

        if (cancelled) {
          return;
        }

        setDashboard(fetchedDashboard);
      } catch {
        if (!cancelled) {
          setLoadFailed(true);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadDashboard();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    setCommunityPageIndex(0);
    setProblemPageIndex(0);
  }, [dashboard.communityPosts, dashboard.problems]);

  function moveCommunityPage(direction: 'previous' | 'next') {
    setCommunityPageIndex((currentPageIndex) => {
      const nextPageIndex = direction === 'previous' ? currentPageIndex - 1 : currentPageIndex + 1;
      return Math.min(communityPageCount - 1, Math.max(0, nextPageIndex));
    });
  }

  function moveProblemPage(direction: 'previous' | 'next') {
    setProblemPageIndex((currentPageIndex) => {
      const nextPageIndex = direction === 'previous' ? currentPageIndex - 1 : currentPageIndex + 1;
      return Math.min(problemPageCount - 1, Math.max(0, nextPageIndex));
    });
  }

  return (
    <div className="page-stack dashboard-page">
      <section className="dashboard-section dashboard-community-section" aria-label="커뮤니티 인기 글">
        <div className="dashboard-section-header">
          <div className="dashboard-section-title">
            <span className="dashboard-section-icon">
              <CommunitySectionIcon />
            </span>
            <h2>커뮤니티 하이라이트</h2>
          </div>

          <CarouselDots activePage={communityPageIndex} label="커뮤니티 페이지 선택" pageCount={communityPageCount} onSelect={setCommunityPageIndex} />

          <button type="button" className="dashboard-section-link" onClick={() => navigate(COMMUNITY_PATH)}>
            전체 보기
            <SectionLinkIcon />
          </button>
        </div>

        {loadFailed ? (
          <PageLoadFailureState className="dashboard-failure-state" />
        ) : (
          <>
            <div className="dashboard-carousel-window dashboard-community-window">
              <button
                type="button"
                className="dashboard-carousel-arrow is-left"
                onClick={() => moveCommunityPage('previous')}
                disabled={communityPageIndex === 0}
                aria-label="이전 커뮤니티 글 보기"
              >
                <CarouselArrowIcon direction="previous" />
              </button>

              <div className={`dashboard-community-grid ${isFeaturedCommunityLayout ? 'is-featured-layout' : 'is-grid-layout'} ${isLoading ? 'is-loading' : ''}`.trim()}>
                {visibleCommunityPosts.length > 0 ? (
                  visibleCommunityPosts.map((post, index) => (
                    <CommunityPostCard key={post.postId} post={post} featured={communityPageIndex === 0 && index === 0} />
                  ))
                ) : isLoading ? (
                  <DashboardCommunityLoadingCards cardCount={communityLoadingCardCount} featuredLayout={isFeaturedCommunityLayout} />
                ) : (
                  <p className="dashboard-empty-text">표시할 게시글이 없습니다.</p>
                )}

                {isLoading ? <DashboardLoadingOverlay /> : null}
              </div>

              <button
                type="button"
                className="dashboard-carousel-arrow is-right"
                onClick={() => moveCommunityPage('next')}
                disabled={communityPageIndex >= communityPageCount - 1}
                aria-label="다음 커뮤니티 글 보기"
              >
                <CarouselArrowIcon direction="next" />
              </button>
            </div>
          </>
        )}
      </section>

      <section className="dashboard-section dashboard-problem-section" aria-label="문제 추천">
        <div className="dashboard-section-header">
          <div className="dashboard-section-title">
            <span className="dashboard-section-icon">
              <ProblemSectionIcon />
            </span>
            <h2>추천 문제</h2>
          </div>

          <CarouselDots activePage={problemPageIndex} label="문제 추천 페이지 선택" pageCount={problemPageCount} onSelect={setProblemPageIndex} />

          <button type="button" className="dashboard-section-link" onClick={() => navigate(PROBLEMS_PATH)}>
            전체 문제 보기
            <SectionLinkIcon />
          </button>
        </div>

        {loadFailed ? (
          <PageLoadFailureState className="dashboard-failure-state" />
        ) : (
          <>
            <div className="dashboard-carousel-window dashboard-problem-window">
              <button
                type="button"
                className="dashboard-carousel-arrow is-left"
                onClick={() => moveProblemPage('previous')}
                disabled={problemPageIndex === 0}
                aria-label="이전 추천 문제 보기"
              >
                <CarouselArrowIcon direction="previous" />
              </button>

              <div className={`dashboard-problem-list ${isLoading ? 'is-loading' : ''}`.trim()}>
                {visibleProblems.length > 0 ? (
                  visibleProblems.map((problem) => (
                    <article key={problem.problemId} className="dashboard-problem-card">
                      <button
                        type="button"
                        className="dashboard-card-hitbox"
                        onClick={() => navigate(buildProblemPath(problem.problemId))}
                        aria-label={`${problem.problemId} 문제로 이동`}
                      />

                      <div className="dashboard-problem-card-head">
                        <span className={`dashboard-dbms-badge is-${problem.dbms}`}>{formatDbmsLabel(problem.dbms)}</span>
                        <h3 className="dashboard-problem-title">{problem.title}</h3>
                      </div>

                      <ProblemMeta problem={problem} />
                    </article>
                  ))
                ) : isLoading ? (
                  <DashboardProblemLoadingRows />
                ) : (
                  <p className="dashboard-empty-text">추천할 문제가 없습니다.</p>
                )}

                {isLoading ? <DashboardLoadingOverlay /> : null}
              </div>

              <button
                type="button"
                className="dashboard-carousel-arrow is-right"
                onClick={() => moveProblemPage('next')}
                disabled={problemPageIndex >= problemPageCount - 1}
                aria-label="다음 추천 문제 보기"
              >
                <CarouselArrowIcon direction="next" />
              </button>
            </div>
          </>
        )}
      </section>
    </div>
  );
}
