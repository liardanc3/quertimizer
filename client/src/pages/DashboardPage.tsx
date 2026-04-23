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
const COMMUNITY_PAGE_SIZES = [3, 4];
const PROBLEM_PAGE_SIZE = 4;
const DASHBOARD_CAROUSEL_PAGE_COUNT = 2;

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
  return (
    <div className="dashboard-loading-overlay" aria-hidden="true">
      <span className="page-loading-spinner dashboard-loading-spinner" />
    </div>
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
  let cursor = 0;

  return COMMUNITY_PAGE_SIZES.map((pageSize) => {
    const pagePosts = posts.slice(cursor, cursor + pageSize);
    cursor += pageSize;
    return pagePosts;
  });
}

function createProblemPages(problems: DashboardProblemRecommendation[]) {
  return Array.from({ length: DASHBOARD_CAROUSEL_PAGE_COUNT }, (_, pageIndex) => {
    const startIndex = pageIndex * PROBLEM_PAGE_SIZE;
    return problems.slice(startIndex, startIndex + PROBLEM_PAGE_SIZE);
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
        <span>편차</span>
        <strong>{`${percentFormatter.format(problem.spreadRate)}%`}</strong>
      </span>
    </div>
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
        <div className="dashboard-section-control">
          <div className="dashboard-section-rail is-community">
            <CarouselDots activePage={communityPageIndex} label="커뮤니티 페이지 선택" pageCount={communityPageCount} onSelect={setCommunityPageIndex} />
          </div>

          <button type="button" className="dashboard-section-link" onClick={() => navigate(COMMUNITY_PATH)}>
            전체 보기
          </button>
        </div>

        {loadFailed ? (
          <PageLoadFailureState className="dashboard-failure-state" />
        ) : (
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

            <div className={`dashboard-community-grid ${communityPageIndex === 0 ? 'is-featured-layout' : 'is-even-layout'} ${isLoading ? 'is-loading' : ''}`.trim()}>
              {visibleCommunityPosts.length > 0 ? (
                visibleCommunityPosts.map((post, index) => (
                  <article key={post.postId} className={`dashboard-post-card ${communityPageIndex === 0 && index === 0 ? 'is-featured' : ''}`.trim()}>
                    <button
                      type="button"
                      className="dashboard-card-hitbox"
                      onClick={() => navigate(getCommunityPostPath(post.postId))}
                      aria-label={`${post.title} 게시글로 이동`}
                    />

                    <div className="dashboard-post-headline">
                      <span className="dashboard-post-category">{getCategoryLabel(post.category)}</span>
                      <h2 className="dashboard-post-title">{post.title}</h2>

                      <div className="dashboard-post-metrics">
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

                    {post.tags.length > 0 ? (
                      <div className="dashboard-post-tags" aria-label="게시글 태그">
                        {post.tags.slice(0, 4).map((tag) => (
                          <span key={tag}>{`#${tag}`}</span>
                        ))}
                      </div>
                    ) : null}

                    <p className="dashboard-post-excerpt">{post.excerpt || '본문 미리보기가 없습니다.'}</p>
                  </article>
                ))
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
        )}
      </section>

      <section className="dashboard-section dashboard-problem-section" aria-label="문제 추천">
        <div className="dashboard-section-control">
          <div className={`dashboard-section-rail is-problem ${dashboard.authenticated ? 'is-personalized' : ''}`.trim()}>
            <CarouselDots activePage={problemPageIndex} label="문제 추천 페이지 선택" pageCount={problemPageCount} onSelect={setProblemPageIndex} />
          </div>

          <button type="button" className="dashboard-section-link" onClick={() => navigate(PROBLEMS_PATH)}>
            문제 목록
          </button>
        </div>

        {loadFailed ? (
          <PageLoadFailureState className="dashboard-failure-state" />
        ) : (
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
                  <article key={problem.problemId} className="dashboard-problem-row">
                    <button
                      type="button"
                      className="dashboard-card-hitbox"
                      onClick={() => navigate(buildProblemPath(problem.problemId))}
                      aria-label={`${problem.problemId} 문제로 이동`}
                    />

                    <span className={`dashboard-dbms-badge is-${problem.dbms}`}>{formatDbmsLabel(problem.dbms)}</span>
                    <span className="dashboard-problem-id">{problem.problemId}</span>
                    <h3 className="dashboard-problem-title">{problem.title}</h3>
                    <ProblemMeta problem={problem} />
                  </article>
                ))
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
        )}
      </section>
    </div>
  );
}
