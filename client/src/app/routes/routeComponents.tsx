import { lazy, Suspense, type ReactNode } from 'react';
import { PageLoading } from '@/shared/ui';
import type { AppRoute } from '@/app/routes/routeConfig';
import RouteErrorBoundary from '@/app/routes/RouteErrorBoundary';

const AdminPage = lazy(() => import('@/pages/admin/ui/AdminPage'));
const CommunityDetailPage = lazy(() => import('@/pages/community/ui/CommunityDetailPage'));
const CommunityPage = lazy(() => import('@/pages/community/ui/CommunityPage'));
const CommunityWritePage = lazy(() => import('@/pages/community/ui/CommunityWritePage'));
const DashboardPage = lazy(() => import('@/pages/dashboard/ui/DashboardPage'));
const FavoritePage = lazy(() => import('@/pages/favorites/ui/FavoritePage'));
const HomePage = lazy(() => import('@/pages/problems/ui/HomePage'));
const ProblemSolvePage = lazy(() => import('@/pages/problem-solve/ui/ProblemSolvePage'));
const ProfileActivityPage = lazy(() => import('@/pages/profile/ui/ProfileActivityPage'));
const ProfilePage = lazy(() => import('@/pages/profile/ui/ProfilePage'));
const RankingPage = lazy(() => import('@/pages/ranking/ui/RankingPage'));
const SocialLoginCallbackPage = lazy(() => import('@/pages/social-login-callback/ui/SocialLoginCallbackPage'));
const SubmitHistoryPage = lazy(() => import('@/pages/submit-history/ui/SubmitHistoryPage'));

function renderWithRouteSuspense(routeKey: string, component: ReactNode) {
  return (
    <RouteErrorBoundary routeKey={routeKey}>
      <Suspense fallback={<PageLoading />}>
        {component}
      </Suspense>
    </RouteErrorBoundary>
  );
}

export function renderSocialLoginCallbackComponent() {
  return renderWithRouteSuspense('social-login-callback', <SocialLoginCallbackPage />);
}

export function renderRouteComponent(route: AppRoute) {
  return renderWithRouteSuspense(resolveRouteKey(route), (() => {
    switch (route.type) {
      case 'problem':
        return <ProblemSolvePage key={route.problemId} problemId={route.problemId} />;
      case 'dashboard':
      case 'home':
        return <DashboardPage />;
      case 'ranking':
        return <RankingPage />;
      case 'submitHistory':
        return <SubmitHistoryPage />;
      case 'favorites':
        return <FavoritePage />;
      case 'community':
        return <CommunityPage />;
      case 'admin':
        return <AdminPage />;
      case 'communityWrite':
        return <CommunityWritePage key="write" />;
      case 'communityEdit':
        return <CommunityWritePage key={`edit-${route.postId}`} postId={route.postId} />;
      case 'communityDetail':
        return <CommunityDetailPage key={route.postId} postId={route.postId} />;
      case 'profileActivity':
        return <ProfileActivityPage key={route.handle ?? 'me'} handle={route.handle} />;
      case 'profile':
        return <ProfilePage key={route.handle ?? 'me'} handle={route.handle} />;
      case 'problems':
        return <HomePage />;
      default:
        return <DashboardPage />;
    }
  })());
}

function resolveRouteKey(route: AppRoute) {
  switch (route.type) {
    case 'problem':
      return `problem:${route.problemId}`;
    case 'communityDetail':
      return `community-detail:${route.postId}`;
    case 'communityEdit':
      return `community-edit:${route.postId}`;
    case 'profile':
      return `profile:${route.handle ?? 'me'}`;
    case 'profileActivity':
      return `profile-activity:${route.handle ?? 'me'}`;
    default:
      return route.type;
  }
}
