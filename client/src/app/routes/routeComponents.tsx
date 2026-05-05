import { lazy, Suspense, type ReactNode } from 'react';
import { PageLoading } from '@/shared/ui';
import type { AppRoute } from '@/app/routes/routeConfig';

const AdminPage = lazy(() => import('@/pages/admin/ui/AdminPage'));
const CommunityDetailPage = lazy(() => import('@/pages/community/ui/CommunityDetailPage'));
const CommunityPage = lazy(() => import('@/pages/community/ui/CommunityPage'));
const CommunityWritePage = lazy(() => import('@/pages/community/ui/CommunityWritePage'));
const DashboardPage = lazy(() => import('@/pages/dashboard/ui/DashboardPage'));
const FavoritePage = lazy(() => import('@/pages/favorites/ui/FavoritePage'));
const GuidePage = lazy(() => import('@/pages/guide/ui/GuidePage'));
const HomePage = lazy(() => import('@/pages/problems/ui/HomePage'));
const ProblemSolvePage = lazy(() => import('@/pages/problem-solve/ui/ProblemSolvePage'));
const ProfileActivityPage = lazy(() => import('@/pages/profile/ui/ProfileActivityPage'));
const ProfilePage = lazy(() => import('@/pages/profile/ui/ProfilePage'));
const RankingPage = lazy(() => import('@/pages/ranking/ui/RankingPage'));
const SocialLoginCallbackPage = lazy(() => import('@/pages/social-login-callback/ui/SocialLoginCallbackPage'));
const SubmitHistoryPage = lazy(() => import('@/pages/submit-history/ui/SubmitHistoryPage'));

function renderWithRouteSuspense(component: ReactNode) {
  return (
    <Suspense fallback={<PageLoading />}>
      {component}
    </Suspense>
  );
}

export function renderSocialLoginCallbackComponent() {
  return renderWithRouteSuspense(<SocialLoginCallbackPage />);
}

export function renderRouteComponent(route: AppRoute) {
  return renderWithRouteSuspense((() => {
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
      case 'guide':
        return <GuidePage />;
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
