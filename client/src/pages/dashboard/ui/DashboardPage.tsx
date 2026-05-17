import { useEffect, useMemo, useRef, useState } from 'react';
import type { MouseEvent, MutableRefObject, PointerEvent } from 'react';
import { ContentLoading } from '@/shared/ui';
import { HttpErrorState } from '@/shared/ui';
import { PageLoadFailureState } from '@/shared/ui';
import { getApiErrorStatus, isCommonHttpErrorStatus } from '@/shared/api/api-error';
import {
  fetchDashboard,
  type DashboardCommunityPost,
  type DashboardData,
  type DashboardProblemRecommendation,
} from '@/shared/api/dashboard-api';
import {
  COMMUNITY_PATH,
  PROBLEMS_PATH,
  getCommunityPostPath,
  getProfilePath,
  navigate,
} from '@/shared/config/navigation';
import { formatCompactInteger } from '@/shared/lib/formatters';
import { getUiTextValue, useUiText } from '@/shared/config/ui-text';
import type { DbmsType } from '@/shared/api/domain';
import './DashboardPage.css';

const COMMUNITY_FEATURED_PAGE_SIZE = 4;
const COMMUNITY_GRID_PAGE_SIZE = 9;
const COMMUNITY_MOBILE_MAX_COUNT = 3;
const PROBLEM_PAGE_SIZE = 6;
const PROBLEM_MOBILE_PAGE_SIZE = 3;
const MAX_PROBLEM_RECOMMENDATION_COUNT = 9;
const DASHBOARD_MOBILE_MEDIA_QUERY = '(max-width: 780px)';
const DASHBOARD_SWIPE_THRESHOLD_PX = 44;
const DASHBOARD_DRAG_MAX_OFFSET_PX = 56;

type DashboardCarouselDirection = 'previous' | 'next';
type DashboardCarouselTarget = 'community' | 'problem';

interface DashboardSwipeStart {
  x: number;
  y: number;
}

function createEmptyDashboard(): DashboardData {
  return {
    authenticated: false,
    currentHandle: null,
    communityPosts: [],
    problems: [],
  };
}

function formatDbmsLabel(dbms: DbmsType) {
  return dbms === 'mysql' ? 'MySQL' : 'PostgreSQL';
}

function formatCount(value: number) {
  return formatCompactInteger(value);
}

function getCategoryLabel(category: string) {
  if (category === 'notice') {
    return getUiTextValue('COMMUNITY_CATEGORY_NOTICE_LABEL', '공지');
  }

  if (category === 'question') {
    return getUiTextValue('COMMUNITY_CATEGORY_QUESTION_LABEL', '질문');
  }

  return getUiTextValue('COMMUNITY_CATEGORY_FREE_LABEL', '자유');
}

function buildProblemPath(problemId: string) {
  return `${PROBLEMS_PATH}/${encodeURIComponent(problemId)}`;
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
  const { text } = useUiText();
  return (
    <div className="dashboard-carousel-dots" aria-label={label}>
      {Array.from({ length: pageCount }, (_, pageIndex) => (
        <button
          key={pageIndex}
          type="button"
          className={`dashboard-carousel-dot ${pageIndex === activePage ? 'is-active' : ''}`}
          onClick={() => onSelect(pageIndex)}
          aria-label={text('DASHBOARD_PAGE_BUTTON_LABEL', { page: pageIndex + 1 }, `${pageIndex + 1}페이지 보기`)}
          aria-pressed={pageIndex === activePage}
        />
      ))}
    </div>
  );
}

function createCommunityPages(posts: DashboardCommunityPost[], mobile: boolean) {
  if (mobile) {
    return posts.slice(0, COMMUNITY_MOBILE_MAX_COUNT).map((post) => [post]);
  }

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

function createProblemPages(problems: DashboardProblemRecommendation[], mobile: boolean) {
  const recommendationProblems = problems.slice(0, MAX_PROBLEM_RECOMMENDATION_COUNT);
  const pageSize = mobile ? PROBLEM_MOBILE_PAGE_SIZE : PROBLEM_PAGE_SIZE;
  const pageCount = Math.ceil(recommendationProblems.length / pageSize);

  return Array.from({ length: pageCount }, (_, pageIndex) => {
    const startIndex = pageIndex * pageSize;
    return recommendationProblems.slice(startIndex, startIndex + pageSize);
  });
}

function resolveNextCarouselPage(currentPageIndex: number, direction: DashboardCarouselDirection, pageCount: number) {
  const nextPageIndex = direction === 'previous' ? currentPageIndex - 1 : currentPageIndex + 1;

  return Math.min(pageCount - 1, Math.max(0, nextPageIndex));
}

function getCarouselSlideClass(direction: DashboardCarouselDirection | null) {
  return direction == null ? '' : `is-slide-${direction}`;
}

function clampCarouselDragOffset(deltaX: number) {
  return Math.max(-DASHBOARD_DRAG_MAX_OFFSET_PX, Math.min(DASHBOARD_DRAG_MAX_OFFSET_PX, deltaX));
}

function CommunityMetric({
  type,
  value,
}: {
  type: 'views' | 'likes' | 'comments';
  value: number;
}) {
  const { text } = useUiText();
  const label = type === 'views'
    ? text('COMMUNITY_VIEWS_COLUMN_LABEL', '조회수')
    : type === 'likes'
      ? text('COMMUNITY_LIKES_COLUMN_LABEL', '좋아요')
      : text('COMMUNITY_COMMENTS_COLUMN_LABEL', '댓글');
  const icon = type === 'views' ? <ViewIcon /> : type === 'likes' ? <LikeIcon /> : <CommentIcon />;

  return (
    <span className={`dashboard-post-metric is-${type}`} aria-label={text('DASHBOARD_METRIC_VALUE_LABEL', { label, count: formatCount(value) }, `${label} ${formatCount(value)}`)}>
      {icon}
      <span>{formatCount(value)}</span>
    </span>
  );
}

function ProblemMeta({ problem }: { problem: DashboardProblemRecommendation }) {
  const { text } = useUiText();

  return (
    <div className="dashboard-problem-meta" aria-label={text('DASHBOARD_PROBLEM_METRICS_LABEL', { problemId: problem.problemId }, `${problem.problemId} 추천 지표`)}>
      <span className="dashboard-problem-metric">
        <span>{text('DASHBOARD_METRIC_SOLVED_LABEL', '푼 사람 수')}</span>
        <strong>{`${formatCount(problem.solvedUserCount)}명`}</strong>
      </span>
      <span className="dashboard-problem-metric">
        <span>{text('DASHBOARD_METRIC_SUBMIT_LABEL', '전체 제출')}</span>
        <strong>{`${formatCount(problem.totalSubmitCount)}회`}</strong>
      </span>
      <span className="dashboard-problem-metric">
        <span>{text('DASHBOARD_METRIC_CORRECT_LABEL', '정답 제출')}</span>
        <strong>{`${formatCount(problem.successSubmitCount)}회`}</strong>
      </span>
    </div>
  );
}

function CommunityPostCard({
  featured,
  onOpen,
  post,
}: {
  featured: boolean;
  onOpen: () => void;
  post: DashboardCommunityPost;
}) {
  const { text } = useUiText();

  return (
    <article className={`dashboard-post-card ${featured ? 'is-featured' : 'is-compact'}`.trim()}>
      <button
        type="button"
        className="dashboard-card-hitbox"
        onClick={onOpen}
        aria-label={text('DASHBOARD_POST_OPEN_LABEL', { title: post.title }, `${post.title} 게시글로 이동`)}
      />

      <div className="dashboard-post-topline">
        <span className={`dashboard-post-category is-${post.category}`}>{getCategoryLabel(post.category)}</span>
        <h2 className="dashboard-post-title">{post.title}</h2>
      </div>

      {featured && post.tags.length === 0 ? null : (
        <div
          className={`dashboard-post-tags ${!featured && post.tags.length === 0 ? 'is-empty' : ''}`.trim()}
          aria-label={post.tags.length > 0 ? text('DASHBOARD_POST_TAGS_LABEL', '게시글 태그') : undefined}
          aria-hidden={post.tags.length === 0}
        >
          {post.tags.slice(0, featured ? 4 : 3).map((tag) => (
            <span key={tag}>{`#${tag}`}</span>
          ))}
        </div>
      )}

      {featured ? <p className="dashboard-post-excerpt">{post.excerpt || text('DASHBOARD_EXCERPT_EMPTY', '본문 미리보기가 없습니다.')}</p> : null}

      <div className="dashboard-post-footer">
        <div className="dashboard-post-metrics" aria-label={text('DASHBOARD_POST_REACTION_LABEL', '게시글 반응')}>
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
  const { text } = useUiText();
  const communitySwipeStartRef = useRef<DashboardSwipeStart | null>(null);
  const problemSwipeStartRef = useRef<DashboardSwipeStart | null>(null);
  const suppressCommunityOpenRef = useRef(false);
  const suppressProblemOpenRef = useRef(false);
  const [dashboard, setDashboard] = useState<DashboardData>(createEmptyDashboard);
  const [isLoading, setIsLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [loadFailedMessage, setLoadFailedMessage] = useState<string | null>(null);
  const [loadFailedStatus, setLoadFailedStatus] = useState<number | null>(null);
  const [communityPageIndex, setCommunityPageIndex] = useState(0);
  const [problemPageIndex, setProblemPageIndex] = useState(0);
  const [communitySlideDirection, setCommunitySlideDirection] = useState<DashboardCarouselDirection | null>(null);
  const [problemSlideDirection, setProblemSlideDirection] = useState<DashboardCarouselDirection | null>(null);
  const [communityDragOffsetPx, setCommunityDragOffsetPx] = useState(0);
  const [problemDragOffsetPx, setProblemDragOffsetPx] = useState(0);
  const [isMobileDashboard, setIsMobileDashboard] = useState(false);
  const communityPages = useMemo(
    () => createCommunityPages(dashboard.communityPosts, isMobileDashboard),
    [dashboard.communityPosts, isMobileDashboard],
  );
  const problemPages = useMemo(
    () => createProblemPages(dashboard.problems, isMobileDashboard),
    [dashboard.problems, isMobileDashboard],
  );
  const communityPageCount = Math.max(1, communityPages.length);
  const problemPageCount = Math.max(1, problemPages.length);
  const visibleCommunityPosts = communityPages[communityPageIndex] ?? [];
  const visibleProblems = problemPages[problemPageIndex] ?? [];
  const isFeaturedCommunityLayout = isMobileDashboard || communityPageIndex === 0;

  useEffect(() => {
    document.documentElement.classList.add('is-dashboard-route');

    return () => {
      document.documentElement.classList.remove('is-dashboard-route');
      document.documentElement.classList.remove('is-dashboard-loading');
    };
  }, []);

  useEffect(() => {
    document.documentElement.classList.toggle('is-dashboard-loading', isLoading);

    return () => {
      document.documentElement.classList.remove('is-dashboard-loading');
    };
  }, [isLoading]);

  useEffect(() => {
    let cancelled = false;

    async function loadDashboard() {
      setIsLoading(true);
      setLoadFailed(false);
      setLoadFailedMessage(null);
      setLoadFailedStatus(null);

      try {
        const fetchedDashboard = await fetchDashboard();

        if (cancelled) {
          return;
        }

        setDashboard(fetchedDashboard);
      } catch (error) {
        if (!cancelled) {
          setLoadFailed(true);
          setLoadFailedMessage(error instanceof Error ? error.message : text('HTTP_SERVER_ERROR_MESSAGE', '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'));
          const status = getApiErrorStatus(error);
          setLoadFailedStatus(isCommonHttpErrorStatus(status) ? status : null);
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
    setCommunitySlideDirection(null);
    setProblemSlideDirection(null);
    setCommunityDragOffsetPx(0);
    setProblemDragOffsetPx(0);
  }, [dashboard.communityPosts, dashboard.problems, isMobileDashboard]);

  useEffect(() => {
    const mediaQueryList = window.matchMedia(DASHBOARD_MOBILE_MEDIA_QUERY);
    const updateMobileState = () => setIsMobileDashboard(mediaQueryList.matches);

    updateMobileState();
    mediaQueryList.addEventListener('change', updateMobileState);

    return () => {
      mediaQueryList.removeEventListener('change', updateMobileState);
    };
  }, []);

  function moveCommunityPage(direction: DashboardCarouselDirection) {
    // 커뮤니티 캐러셀 이동 방향 저장 후 페이지 전환
    const nextPageIndex = resolveNextCarouselPage(communityPageIndex, direction, communityPageCount);
    if (nextPageIndex === communityPageIndex) {
      return;
    }

    setCommunitySlideDirection(direction);
    setCommunityPageIndex(nextPageIndex);
  }

  function moveProblemPage(direction: DashboardCarouselDirection) {
    // 추천 문제 캐러셀 이동 방향 저장 후 페이지 전환
    const nextPageIndex = resolveNextCarouselPage(problemPageIndex, direction, problemPageCount);
    if (nextPageIndex === problemPageIndex) {
      return;
    }

    setProblemSlideDirection(direction);
    setProblemPageIndex(nextPageIndex);
  }

  function setCarouselDragOffset(target: DashboardCarouselTarget, offsetPx: number) {
    // 캐러셀별 드래그 오프셋 저장
    if (target === 'community') {
      setCommunityDragOffsetPx(offsetPx);
      return;
    }

    setProblemDragOffsetPx(offsetPx);
  }

  function markSwipeOpenSuppressed(target: 'community' | 'problem') {
    // 스와이프 후 발생하는 카드 click 이동 방지
    const suppressRef = target === 'community' ? suppressCommunityOpenRef : suppressProblemOpenRef;
    suppressRef.current = true;
    window.setTimeout(() => {
      suppressRef.current = false;
    }, 180);
  }

  function handleCarouselPointerDown(
    event: PointerEvent<HTMLDivElement>,
    swipeStartRef: MutableRefObject<DashboardSwipeStart | null>,
    target: DashboardCarouselTarget,
  ) {
    // 모바일 터치 시작점 저장
    if (!isMobileDashboard || loadFailed || event.pointerType === 'mouse') {
      return;
    }

    swipeStartRef.current = { x: event.clientX, y: event.clientY };
    setCarouselDragOffset(target, 0);
  }

  function handleCarouselPointerMove(
    event: PointerEvent<HTMLDivElement>,
    swipeStartRef: MutableRefObject<DashboardSwipeStart | null>,
    target: DashboardCarouselTarget,
  ) {
    // 모바일 수평 드래그 거리만 카드 오프셋으로 반영
    if (!isMobileDashboard || event.pointerType === 'mouse' || swipeStartRef.current == null) {
      return;
    }

    const deltaX = event.clientX - swipeStartRef.current.x;
    const deltaY = event.clientY - swipeStartRef.current.y;
    if (Math.abs(deltaX) < Math.abs(deltaY)) {
      return;
    }

    setCarouselDragOffset(target, clampCarouselDragOffset(deltaX));
  }

  function handleCarouselPointerUp(
    event: PointerEvent<HTMLDivElement>,
    swipeStartRef: MutableRefObject<DashboardSwipeStart | null>,
    movePage: (direction: DashboardCarouselDirection) => void,
    target: DashboardCarouselTarget,
  ) {
    // 수평 스와이프를 페이지 이동으로 변환
    if (!isMobileDashboard || event.pointerType === 'mouse') {
      swipeStartRef.current = null;
      setCarouselDragOffset(target, 0);
      return;
    }

    const swipeStart = swipeStartRef.current;
    swipeStartRef.current = null;
    setCarouselDragOffset(target, 0);
    if (swipeStart == null) {
      return;
    }

    const deltaX = event.clientX - swipeStart.x;
    const deltaY = event.clientY - swipeStart.y;
    if (Math.abs(deltaX) < DASHBOARD_SWIPE_THRESHOLD_PX || Math.abs(deltaX) < Math.abs(deltaY) * 1.2) {
      return;
    }

    markSwipeOpenSuppressed(target);
    movePage(deltaX < 0 ? 'next' : 'previous');
  }

  function handleCarouselPointerCancel(
    swipeStartRef: MutableRefObject<DashboardSwipeStart | null>,
    target: DashboardCarouselTarget,
  ) {
    // 취소된 터치 시작점 제거
    swipeStartRef.current = null;
    setCarouselDragOffset(target, 0);
  }

  function handleCarouselClickCapture(event: MouseEvent<HTMLDivElement>, target: DashboardCarouselTarget) {
    // 스와이프 직후 발생한 내부 button click 차단
    const suppressRef = target === 'community' ? suppressCommunityOpenRef : suppressProblemOpenRef;
    if (!suppressRef.current) {
      return;
    }

    suppressRef.current = false;
    event.preventDefault();
    event.stopPropagation();
  }

  function openCommunityPost(postId: string) {
    // 스와이프 직후 발생한 click이면 이동 생략
    if (suppressCommunityOpenRef.current) {
      return;
    }

    navigate(getCommunityPostPath(postId));
  }

  function openProblem(problemId: string) {
    // 스와이프 직후 발생한 click이면 이동 생략
    if (suppressProblemOpenRef.current) {
      return;
    }

    navigate(buildProblemPath(problemId));
  }

  if (isLoading) {
    return <ContentLoading as="section" className="dashboard-page-loading-shell" />;
  }

  return (
    <div className="page-stack dashboard-page">
      <section className="dashboard-section dashboard-community-section" aria-label={text('DASHBOARD_COMMUNITY_SECTION_LABEL', '커뮤니티 인기 글')}>
        <div className="dashboard-section-header">
          <button type="button" className="dashboard-section-title dashboard-section-title-button" onClick={() => navigate(COMMUNITY_PATH)}>
            <span className="dashboard-section-icon">
              <CommunitySectionIcon />
            </span>
            <h2>{text('DASHBOARD_HIGHLIGHT_SECTION_TITLE', '커뮤니티 하이라이트')}</h2>
          </button>

          <CarouselDots activePage={communityPageIndex} label={text('DASHBOARD_COMMUNITY_PAGE_LABEL', '커뮤니티 페이지 선택')} pageCount={communityPageCount} onSelect={setCommunityPageIndex} />

          <button type="button" className="dashboard-section-link" onClick={() => navigate(COMMUNITY_PATH)}>
            {text('DASHBOARD_ALL_VIEW_BUTTON', '전체 보기')}
            <SectionLinkIcon />
          </button>
        </div>

        <div
          className="dashboard-carousel-window dashboard-community-window"
          onPointerDown={(event) => handleCarouselPointerDown(event, communitySwipeStartRef, 'community')}
          onPointerMove={(event) => handleCarouselPointerMove(event, communitySwipeStartRef, 'community')}
          onPointerUp={(event) => handleCarouselPointerUp(event, communitySwipeStartRef, moveCommunityPage, 'community')}
          onPointerCancel={() => handleCarouselPointerCancel(communitySwipeStartRef, 'community')}
          onClickCapture={(event) => handleCarouselClickCapture(event, 'community')}
        >
          <button
            type="button"
            className="dashboard-carousel-arrow is-left"
            onClick={() => moveCommunityPage('previous')}
            disabled={loadFailed || communityPageIndex === 0}
            aria-label={text('DASHBOARD_COMMUNITY_PREVIOUS_BUTTON_LABEL', '이전 커뮤니티 글 보기')}
          >
            <CarouselArrowIcon direction="previous" />
          </button>

          <div
            key={`community-${communityPageIndex}`}
            className={`dashboard-community-grid dashboard-carousel-slide ${isFeaturedCommunityLayout ? 'is-featured-layout' : 'is-grid-layout'} ${getCarouselSlideClass(communitySlideDirection)}`.trim()}
            style={communityDragOffsetPx !== 0 ? { transform: `translateX(${communityDragOffsetPx}px)` } : undefined}
            onAnimationEnd={() => setCommunitySlideDirection(null)}
          >
            {loadFailed ? (
              loadFailedStatus != null
                ? <HttpErrorState status={loadFailedStatus} className="dashboard-empty-text dashboard-section-error" message={loadFailedMessage} />
                : <PageLoadFailureState className="dashboard-empty-text dashboard-section-error" message={loadFailedMessage} />
            ) : visibleCommunityPosts.length > 0 ? (
              visibleCommunityPosts.map((post, index) => (
                <CommunityPostCard
                  key={post.postId}
                  post={post}
                  featured={isMobileDashboard || (communityPageIndex === 0 && index === 0)}
                  onOpen={() => openCommunityPost(post.postId)}
                />
              ))
            ) : (
              <p className="dashboard-empty-text">{text('DASHBOARD_EMPTY_POSTS_STATE', '표시할 게시글이 없습니다.')}</p>
            )}
          </div>

          <button
            type="button"
            className="dashboard-carousel-arrow is-right"
            onClick={() => moveCommunityPage('next')}
            disabled={loadFailed || communityPageIndex >= communityPageCount - 1}
            aria-label={text('DASHBOARD_COMMUNITY_NEXT_BUTTON_LABEL', '다음 커뮤니티 글 보기')}
          >
            <CarouselArrowIcon direction="next" />
          </button>
        </div>
      </section>

      <section className="dashboard-section dashboard-problem-section" aria-label={text('DASHBOARD_PROBLEM_SECTION_LABEL', '문제 추천')}>
        <div className="dashboard-section-header">
          <button type="button" className="dashboard-section-title dashboard-section-title-button" onClick={() => navigate(PROBLEMS_PATH)}>
            <span className="dashboard-section-icon">
              <ProblemSectionIcon />
            </span>
            <h2>{text('DASHBOARD_RECOMMEND_SECTION_TITLE', '추천 문제')}</h2>
          </button>

          <CarouselDots activePage={problemPageIndex} label={text('DASHBOARD_PROBLEM_PAGE_LABEL', '문제 추천 페이지 선택')} pageCount={problemPageCount} onSelect={setProblemPageIndex} />

          <button type="button" className="dashboard-section-link" onClick={() => navigate(PROBLEMS_PATH)}>
            {text('DASHBOARD_ALL_PROBLEMS_BUTTON', '전체 문제 보기')}
            <SectionLinkIcon />
          </button>
        </div>

        <div
          className="dashboard-carousel-window dashboard-problem-window"
          onPointerDown={(event) => handleCarouselPointerDown(event, problemSwipeStartRef, 'problem')}
          onPointerMove={(event) => handleCarouselPointerMove(event, problemSwipeStartRef, 'problem')}
          onPointerUp={(event) => handleCarouselPointerUp(event, problemSwipeStartRef, moveProblemPage, 'problem')}
          onPointerCancel={() => handleCarouselPointerCancel(problemSwipeStartRef, 'problem')}
          onClickCapture={(event) => handleCarouselClickCapture(event, 'problem')}
        >
          <button
            type="button"
            className="dashboard-carousel-arrow is-left"
            onClick={() => moveProblemPage('previous')}
            disabled={loadFailed || problemPageIndex === 0}
            aria-label={text('DASHBOARD_PROBLEM_PREVIOUS_BUTTON_LABEL', '이전 추천 문제 보기')}
          >
            <CarouselArrowIcon direction="previous" />
          </button>

          <div
            key={`problem-${problemPageIndex}`}
            className={`dashboard-problem-list dashboard-carousel-slide ${getCarouselSlideClass(problemSlideDirection)}`.trim()}
            style={problemDragOffsetPx !== 0 ? { transform: `translateX(${problemDragOffsetPx}px)` } : undefined}
            onAnimationEnd={() => setProblemSlideDirection(null)}
          >
            {loadFailed ? (
              loadFailedStatus != null
                ? <HttpErrorState status={loadFailedStatus} className="dashboard-empty-text dashboard-section-error" message={loadFailedMessage} />
                : <PageLoadFailureState className="dashboard-empty-text dashboard-section-error" message={loadFailedMessage} />
            ) : visibleProblems.length > 0 ? (
              visibleProblems.map((problem) => (
                <article key={problem.problemId} className="dashboard-problem-card">
                  <button
                    type="button"
                    className="dashboard-card-hitbox"
                    onClick={() => openProblem(problem.problemId)}
                    aria-label={text('DASHBOARD_PROBLEM_OPEN_LABEL', { problemId: problem.problemId }, `${problem.problemId} 문제로 이동`)}
                  />

                  <div className="dashboard-problem-card-head">
                    <span className={`dashboard-dbms-badge is-${problem.dbms}`}>{formatDbmsLabel(problem.dbms)}</span>
                    <h3 className="dashboard-problem-title">{problem.title}</h3>
                  </div>

                  <ProblemMeta problem={problem} />
                </article>
              ))
            ) : (
              <p className="dashboard-empty-text">{text('DASHBOARD_EMPTY_PROBLEMS_STATE', '추천할 문제가 없습니다.')}</p>
            )}
          </div>

          <button
            type="button"
            className="dashboard-carousel-arrow is-right"
            onClick={() => moveProblemPage('next')}
            disabled={loadFailed || problemPageIndex >= problemPageCount - 1}
            aria-label={text('DASHBOARD_PROBLEM_NEXT_BUTTON_LABEL', '다음 추천 문제 보기')}
          >
            <CarouselArrowIcon direction="next" />
          </button>
        </div>
      </section>
    </div>
  );
}
