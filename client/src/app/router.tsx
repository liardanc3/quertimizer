import { useEffect } from 'react';
import { useLocationPathname, useLocationSearch } from '@/shared/lib/hooks/use-location-state';
import { openLoginOverlay } from '@/shared/auth/auth-overlay';
import { COMMUNITY_PATH, DASHBOARD_PATH, PROBLEMS_PATH, getProfileActivityPath, getProfilePath, navigate } from '@/shared/config/navigation';
import { useSession } from '@/shared/auth/session';
import { hasSocialLoginCallbackSearch } from '@/shared/auth/social-login-callback';
import { useHomeSiteTitle, useUiText } from '@/shared/config/ui-text';
import { type AppRoute, parseRoute, routeNeedsSession } from '@/app/routes/routeConfig';
import { renderRouteComponent, renderSocialLoginCallbackComponent } from '@/app/routes/routeComponents';

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
  const { isAuthenticated, isReady, isAdmin, handleSetupRequired, handle: currentHandle, reauthenticationRequired } = useSession();
  const shouldRequireHandleSetup = isAuthenticated && handleSetupRequired;
  const needsSession = routeNeedsSession(route);
  const shouldPreserveRouteForReauthentication = needsSession && reauthenticationRequired && !isAuthenticated;
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
    if ((route.type !== 'communityWrite' && route.type !== 'communityEdit') || isAuthenticated || !isReady || reauthenticationRequired) {
      return;
    }

    openLoginOverlay(text('ROUTER_COMMUNITY_WRITE_LOGIN_RETURN_MESSAGE', '로그인 후 커뮤니티 작성 화면으로 돌아옵니다.'), { force: true });
    navigate(COMMUNITY_PATH, { replace: true });
  }, [isAuthenticated, isReady, reauthenticationRequired, route.type, text]);

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
    return renderSocialLoginCallbackComponent();
  }

  if (needsSession && !isReady) {
    return renderRouteComponent(route);
  }

  if (shouldRequireHandleSetup) {
    if (route.type === 'problem') {
      return renderRouteComponent(route);
    }

    return renderRouteComponent({ type: 'problems' });
  }

  if (route.type === 'notFound') {
    return (
      <div className="page-stack route-inline-state-layout">
        <p className="route-inline-state-message" role="status">
          {text('ROUTER_NOT_FOUND_TITLE', '찾을 수 없는 페이지입니다.')}
        </p>
      </div>
    );
  }

  if (shouldPreserveRouteForReauthentication) {
    return renderRouteComponent(route);
  }

  if (route.type === 'admin' && !(isAuthenticated && isAdmin)) {
    return (
      <div className="page-stack route-inline-state-layout">
        <p className="route-inline-state-message" role="status">
          {text('HTTP_FORBIDDEN_ERROR_MESSAGE', '접근 권한이 없습니다.')}
        </p>
      </div>
    );
  }

  if ((route.type === 'communityWrite' || route.type === 'communityEdit') && !isAuthenticated) {
    return null;
  }

  if ((route.type === 'profile' || route.type === 'profileActivity') && route.handle == null) {
    return null;
  }

  return renderRouteComponent(route);
}
