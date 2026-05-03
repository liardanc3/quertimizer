import { useEffect } from 'react';
import PageStatePanel from './components/common/PageStatePanel';
import CommunityWritePage from './pages/CommunityWritePage';
import HomePage from './pages/HomePage';
import ProblemSolvePage from './pages/ProblemSolvePage';
import AdminPage from './pages/AdminPage';
import SocialLoginCallbackPage from './pages/SocialLoginCallbackPage';
import { useLocationPathname, useLocationSearch } from './hooks/useLocationState';
import { openLoginOverlay } from './lib/authOverlay';
import { DASHBOARD_PATH, PROBLEMS_PATH, getProfileActivityPath, getProfilePath, navigate } from './lib/navigation';
import { useSession } from './lib/session';
import { hasSocialLoginCallbackSearch } from './lib/socialLoginCallback';
import { useHomeSiteTitle, useUiText } from './lib/uiText';
import { type AppRoute, parseRoute, routeNeedsSession } from './routes/routeConfig';
import { renderRouteComponent } from './routes/routeComponents';

function resolveRoutePageTitle(route: AppRoute, text: ReturnType<typeof useUiText>['text']) {
  switch (route.type) {
    case 'dashboard':
    case 'home':
      return text('PAGE_TITLE_DASHBOARD', '대시보드');
    case 'problems':
      return text('PAGE_TITLE_PROBLEMS', '문제');
    case 'problem':
      return text('PAGE_TITLE_PROBLEM_DETAIL', { problemId: route.problemId }, `문제 ${route.problemId}`);
    case 'submitHistory':
      return text('PAGE_TITLE_SUBMISSIONS', '제출 목록');
    case 'favorites':
      return text('PAGE_TITLE_FAVORITES', '즐겨찾기');
    case 'ranking':
      return text('PAGE_TITLE_RANKING', '랭킹');
    case 'community':
      return text('PAGE_TITLE_COMMUNITY', '커뮤니티');
    case 'communityDetail':
      return text('PAGE_TITLE_COMMUNITY_DETAIL', '커뮤니티 게시글');
    case 'communityWrite':
      return text('PAGE_TITLE_COMMUNITY_WRITE', '게시글 작성');
    case 'communityEdit':
      return text('PAGE_TITLE_COMMUNITY_EDIT', '게시글 수정');
    case 'guide':
      return text('PAGE_TITLE_GUIDE', '가이드');
    case 'admin':
      return text('PAGE_TITLE_ADMIN', '관리자');
    case 'profile':
      return route.handle
        ? text('PAGE_TITLE_PROFILE_HANDLE', { handle: route.handle }, `${route.handle} 프로필`)
        : text('PAGE_TITLE_PROFILE', '프로필');
    case 'profileActivity':
      return route.handle
        ? text('PAGE_TITLE_PROFILE_ACTIVITY_HANDLE', { handle: route.handle }, `${route.handle} 활동 기록`)
        : text('PAGE_TITLE_PROFILE_ACTIVITY', '활동 기록');
    default:
      return text('PAGE_TITLE_NOT_FOUND', '찾을 수 없는 페이지');
  }
}

export default function AppRouter() {
  const { text } = useUiText();
  const pathname = useLocationPathname();
  const search = useLocationSearch();
  const route = parseRoute(pathname);
  const { isAuthenticated, isReady, isAdmin, handleSetupRequired, handle: currentHandle } = useSession();
  const shouldRequireHandleSetup = isAuthenticated && handleSetupRequired;
  const needsSession = routeNeedsSession(route);
  const isSocialLoginCallback = route.type === 'home' && hasSocialLoginCallbackSearch(search);
  const routePageTitle = resolveRoutePageTitle(route, text);
  const siteTitle = text('TITLE', '쿼티마이저');

  useHomeSiteTitle(text('PAGE_TITLE_FORMAT', { page: routePageTitle, site: siteTitle }, `${routePageTitle} | ${siteTitle}`));

  useEffect(() => {
    if (route.type === 'home' && !isSocialLoginCallback) {
      navigate(DASHBOARD_PATH, { replace: true });
    }
  }, [isSocialLoginCallback, pathname, route.type]);

  useEffect(() => {
    if (!shouldRequireHandleSetup) {
      return;
    }

    if (window.location.pathname === PROBLEMS_PATH || route.type === 'problem') {
      return;
    }

    navigate(PROBLEMS_PATH, { replace: true });
  }, [pathname, route.type, shouldRequireHandleSetup]);

  useEffect(() => {
    if (route.type === 'profile' && route.handle == null) {
      if (isAuthenticated && currentHandle != null) {
        navigate(getProfilePath(currentHandle), { replace: true });
        return;
      }

      if (isReady && !isAuthenticated) {
        navigate(DASHBOARD_PATH, { replace: true });
      }
    }

    if (route.type === 'profileActivity' && route.handle == null) {
      if (isAuthenticated && currentHandle != null) {
        navigate(getProfileActivityPath(currentHandle), { replace: true });
        return;
      }

      if (isReady && !isAuthenticated) {
        navigate(DASHBOARD_PATH, { replace: true });
      }
    }
  }, [currentHandle, isAuthenticated, isReady, route]);

  if (isSocialLoginCallback) {
    return <SocialLoginCallbackPage />;
  }

  if (needsSession && !isReady) {
    if (route.type === 'admin') {
      return <AdminPage />;
    }

    if (route.type === 'communityWrite') {
      return <CommunityWritePage key="write" />;
    }

    if (route.type === 'communityEdit') {
      return <CommunityWritePage key={`edit-${route.postId}`} postId={route.postId} />;
    }

  }

  if (shouldRequireHandleSetup) {
    if (route.type === 'problem') {
      return <ProblemSolvePage key={route.problemId} problemId={route.problemId} />;
    }

    return <HomePage />;
  }

  if (route.type === 'notFound') {
    return (
      <PageStatePanel
        fullPage
        label="404"
        title={text('ROUTER_NOT_FOUND_TITLE', '찾을 수 없는 페이지입니다.')}
        description={text('ROUTER_NOT_FOUND_DESC', '주소가 바뀌었거나 삭제된 화면입니다. 대시보드에서 다시 이동해 주세요.')}
        actionLabel={text('PROFILE_DASHBOARD_MOVE_BUTTON', '대시보드로 이동')}
        onAction={() => navigate(DASHBOARD_PATH, { replace: true })}
      />
    );
  }

  if (route.type === 'admin' && !(isAuthenticated && isAdmin)) {
    return (
      <PageStatePanel
        fullPage
        label={text('ROUTER_ACCESS_DENIED_LABEL', '권한')}
        title={text('ROUTER_ADMIN_ACCESS_DENIED_TITLE', '관리자 화면에 접근할 수 없습니다.')}
        description={text('ADMIN_ACCESS_DENIED_MESSAGE', '관리자만 접근할 수 있습니다.')}
        actionLabel={isAuthenticated ? text('PROFILE_DASHBOARD_MOVE_BUTTON', '대시보드로 이동') : text('AUTH_LOGIN_TITLE', '로그인')}
        onAction={
          isAuthenticated
            ? () => navigate(DASHBOARD_PATH, { replace: true })
            : () => openLoginOverlay(text('ROUTER_ADMIN_LOGIN_REQUIRED_MESSAGE', '관리자 화면은 권한이 있는 계정으로만 접근할 수 있습니다.'))
        }
      />
    );
  }

  if ((route.type === 'communityWrite' || route.type === 'communityEdit') && !isAuthenticated) {
    return (
      <PageStatePanel
        fullPage
        label={text('AUTH_LOGIN_TITLE', '로그인')}
        title={
          route.type === 'communityEdit'
            ? text('ROUTER_COMMUNITY_EDIT_LOGIN_REQUIRED_TITLE', '로그인 후 게시글을 수정할 수 있습니다.')
            : text('ROUTER_COMMUNITY_WRITE_LOGIN_REQUIRED_TITLE', '로그인 후 게시글을 작성할 수 있습니다.')
        }
        description={text('ROUTER_COMMUNITY_WRITE_LOGIN_REQUIRED_DESC', '커뮤니티 작성 화면은 로그인한 사용자에게만 열립니다.')}
        actionLabel={text('AUTH_LOGIN_TITLE', '로그인')}
        onAction={() => openLoginOverlay(text('ROUTER_COMMUNITY_WRITE_LOGIN_RETURN_MESSAGE', '로그인 후 커뮤니티 작성 화면으로 돌아옵니다.'))}
      />
    );
  }

  if ((route.type === 'profile' || route.type === 'profileActivity') && route.handle == null) {
    return null;
  }

  return renderRouteComponent(route);
}
