import { useEffect } from 'react';
import PageStatePanel from './components/common/PageStatePanel';
import CommunityWritePage from './pages/CommunityWritePage';
import HomePage from './pages/HomePage';
import ProfilePage from './pages/ProfilePage';
import ProfileActivityPage from './pages/ProfileActivityPage';
import ProblemSolvePage from './pages/ProblemSolvePage';
import AdminPage from './pages/AdminPage';
import SocialLoginCallbackPage from './pages/SocialLoginCallbackPage';
import { useLocationPathname, useLocationSearch } from './hooks/useLocationState';
import { openLoginOverlay } from './lib/authOverlay';
import { DASHBOARD_PATH, PROBLEMS_PATH, navigate } from './lib/navigation';
import { useSession } from './lib/session';
import { hasSocialLoginCallbackSearch } from './lib/socialLoginCallback';
import { useUiText } from './lib/uiText';
import { parseRoute, routeNeedsSession } from './routes/routeConfig';
import { renderRouteComponent } from './routes/routeComponents';

export default function AppRouter() {
  const { text } = useUiText();
  const pathname = useLocationPathname();
  const search = useLocationSearch();
  const route = parseRoute(pathname);
  const { isAuthenticated, isReady, isAdmin, isProblemGenerator, handleSetupRequired } = useSession();
  const shouldRequireHandleSetup = isAuthenticated && handleSetupRequired;
  const needsSession = routeNeedsSession(route);
  const isSocialLoginCallback = route.type === 'home' && hasSocialLoginCallbackSearch(search);

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

    if (route.type === 'profileActivity') {
      return <ProfileActivityPage key={route.handle ?? 'me'} handle={route.handle} />;
    }

    if (route.type === 'profile') {
      return <ProfilePage key={route.handle ?? 'me'} handle={route.handle} />;
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

  if (route.type === 'admin' && !(isAuthenticated && (isAdmin || isProblemGenerator))) {
    return (
      <PageStatePanel
        fullPage
        label={text('ROUTER_ACCESS_DENIED_LABEL', '권한')}
        title={text('ROUTER_ADMIN_ACCESS_DENIED_TITLE', '관리자 화면에 접근할 수 없습니다.')}
        description={text('ADMIN_ACCESS_DENIED_MESSAGE', '관리자 또는 ProblemGenerator만 접근할 수 있습니다.')}
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

  if (route.type === 'profile' && route.handle == null && !isAuthenticated) {
    return (
      <PageStatePanel
        fullPage
        label={text('PROFILE_PAGE_LABEL', '프로필')}
        title={text('PROFILE_LOGIN_REQUIRED_TITLE', '내 프로필을 보려면 로그인이 필요합니다.')}
        description={text('PROFILE_LOGIN_REQUIRED_DESC', '로그인 후 프로필과 활동 기록을 다시 확인해 주세요.')}
        actionLabel={text('AUTH_LOGIN_TITLE', '로그인')}
        onAction={() => openLoginOverlay(text('PROFILE_LOGIN_RETURN_MESSAGE', '로그인 후 내 프로필 화면으로 돌아옵니다.'))}
      />
    );
  }

  if (route.type === 'profileActivity' && route.handle == null && !isAuthenticated) {
    return (
      <PageStatePanel
        fullPage
        label={text('PROFILE_ACTIVITY_PAGE_LABEL', '활동 기록')}
        title={text('PROFILE_ACTIVITY_LOGIN_REQUIRED_TITLE', '내 활동 기록을 보려면 로그인이 필요합니다.')}
        description={text('PROFILE_ACTIVITY_LOGIN_REQUIRED_DESC', '로그인 후 작성한 글, 댓글, 좋아요 기록을 다시 확인해 주세요.')}
        actionLabel={text('AUTH_LOGIN_TITLE', '로그인')}
        onAction={() => openLoginOverlay(text('PROFILE_ACTIVITY_LOGIN_MOVE_MESSAGE', '로그인 후 내 활동 기록 화면으로 이동할 수 있습니다.'))}
      />
    );
  }

  return renderRouteComponent(route);
}
