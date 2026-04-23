import { useEffect, useSyncExternalStore } from 'react';
import AdminPage from './pages/AdminPage';
import CommunityDetailPage from './pages/CommunityDetailPage';
import CommunityPage from './pages/CommunityPage';
import CommunityWritePage from './pages/CommunityWritePage';
import DashboardPage from './pages/DashboardPage';
import FavoritePage from './pages/FavoritePage';
import GuidePage from './pages/GuidePage';
import HomePage from './pages/HomePage';
import ProfilePage from './pages/ProfilePage';
import ProfileActivityPage from './pages/ProfileActivityPage';
import ProblemSolvePage from './pages/ProblemSolvePage';
import RankingPage from './pages/RankingPage';
import SubmitHistoryPage from './pages/SubmitHistoryPage';
import { DASHBOARD_PATH, FAVORITES_PATH, PROBLEMS_PATH, SUBMIT_HISTORY_PATH, navigate } from './lib/navigation';
import { useMockSession } from './lib/session';

interface ProblemRoute {
  type: 'problem';
  problemId: string;
}

interface HomeRoute {
  type: 'home';
}

interface DashboardRoute {
  type: 'dashboard';
}

interface ProblemsRoute {
  type: 'problems';
}

interface RankingRoute {
  type: 'ranking';
}

interface SubmitHistoryRoute {
  type: 'submitHistory';
}

interface FavoritesRoute {
  type: 'favorites';
}

interface CommunityRoute {
  type: 'community';
}

interface GuideRoute {
  type: 'guide';
}

interface AdminRoute {
  type: 'admin';
}

interface CommunityDetailRoute {
  type: 'communityDetail';
  postId: string;
}

interface CommunityWriteRoute {
  type: 'communityWrite';
}

interface CommunityEditRoute {
  type: 'communityEdit';
  postId: string;
}

interface ProfileRoute {
  type: 'profile';
  handle?: string;
}

interface ProfileActivityRoute {
  type: 'profileActivity';
  handle?: string;
}

type AppRoute =
  | HomeRoute
  | DashboardRoute
  | ProblemsRoute
  | SubmitHistoryRoute
  | FavoritesRoute
  | RankingRoute
  | CommunityRoute
  | GuideRoute
  | AdminRoute
  | CommunityWriteRoute
  | CommunityEditRoute
  | CommunityDetailRoute
  | ProfileActivityRoute
  | ProfileRoute
  | ProblemRoute;

function subscribe(callback: () => void) {
  window.addEventListener('popstate', callback);
  return () => window.removeEventListener('popstate', callback);
}

function getSnapshot() {
  return window.location.pathname;
}

function parseRoute(pathname: string): AppRoute {
  const normalizedPathname = pathname !== '/' ? pathname.replace(/\/+$/, '') : pathname;

  if (normalizedPathname === '/') {
    return { type: 'home' };
  }

  if (normalizedPathname === DASHBOARD_PATH) {
    return { type: 'dashboard' };
  }

  if (normalizedPathname === '/problems') {
    return { type: 'problems' };
  }

  if (normalizedPathname === '/problems/create') {
    return { type: 'admin' };
  }

  if (normalizedPathname === '/ranking') {
    return { type: 'ranking' };
  }

  if (normalizedPathname === SUBMIT_HISTORY_PATH) {
    return { type: 'submitHistory' };
  }

  if (normalizedPathname === FAVORITES_PATH) {
    return { type: 'favorites' };
  }

  if (normalizedPathname === '/community') {
    return { type: 'community' };
  }

  if (normalizedPathname === '/guide') {
    return { type: 'guide' };
  }

  if (normalizedPathname === '/admin') {
    return { type: 'admin' };
  }

  if (normalizedPathname === '/community/write') {
    return { type: 'communityWrite' };
  }

  const communityEditMatch = normalizedPathname.match(/^\/community\/([a-zA-Z0-9-]+)\/edit$/);
  if (communityEditMatch) {
    return { type: 'communityEdit', postId: decodeURIComponent(communityEditMatch[1]) };
  }

  const communityPostMatch = normalizedPathname.match(/^\/community\/(?!write$)([a-zA-Z0-9-]+)$/);
  if (communityPostMatch) {
    return { type: 'communityDetail', postId: decodeURIComponent(communityPostMatch[1]) };
  }

  if (normalizedPathname === '/profile') {
    return { type: 'profile' };
  }

  if (normalizedPathname === '/profile/activity') {
    return { type: 'profileActivity' };
  }

  const profileActivityMatch = normalizedPathname.match(/^\/profile\/([\w-]+)\/activity$/);
  if (profileActivityMatch) {
    return { type: 'profileActivity', handle: decodeURIComponent(profileActivityMatch[1]) };
  }

  const profileMatch = normalizedPathname.match(/^\/profile\/([\w-]+)$/);
  if (profileMatch) {
    return { type: 'profile', handle: decodeURIComponent(profileMatch[1]) };
  }

  const problemMatch = normalizedPathname.match(/^\/problems\/([a-zA-Z0-9-]+)$/);
  if (problemMatch) {
    return { type: 'problem', problemId: problemMatch[1] };
  }

  return { type: 'home' };
}

export default function AppRouter() {
  const pathname = useSyncExternalStore(subscribe, getSnapshot, () => '/');
  const route = parseRoute(pathname);
  const { isAuthenticated, handleSetupRequired } = useMockSession();
  const shouldRequireHandleSetup = isAuthenticated && handleSetupRequired;

  useEffect(() => {
    if (route.type === 'home') {
      navigate(DASHBOARD_PATH, { replace: true });
    }
  }, [pathname, route.type]);

  useEffect(() => {
    if (!shouldRequireHandleSetup) {
      return;
    }

    if (window.location.pathname === PROBLEMS_PATH || route.type === 'problem') {
      return;
    }

    navigate(PROBLEMS_PATH, { replace: true });
  }, [pathname, route.type, shouldRequireHandleSetup]);

  if (shouldRequireHandleSetup) {
    if (route.type === 'problem') {
      return <ProblemSolvePage key={route.problemId} problemId={route.problemId} />;
    }

    return <HomePage />;
  }

  if (route.type === 'problem') {
    return <ProblemSolvePage key={route.problemId} problemId={route.problemId} />;
  }

  if (route.type === 'dashboard') {
    return <DashboardPage />;
  }

  if (route.type === 'home') {
    return <DashboardPage />;
  }

  if (route.type === 'ranking') {
    return <RankingPage />;
  }

  if (route.type === 'submitHistory') {
    return <SubmitHistoryPage />;
  }

  if (route.type === 'favorites') {
    return <FavoritePage />;
  }

  if (route.type === 'community') {
    return <CommunityPage />;
  }

  if (route.type === 'guide') {
    return <GuidePage />;
  }

  if (route.type === 'admin') {
    return <AdminPage />;
  }

  if (route.type === 'communityWrite') {
    return <CommunityPage />;
  }

  if (route.type === 'communityEdit') {
    return <CommunityWritePage key={`edit-${route.postId}`} postId={route.postId} />;
  }

  if (route.type === 'communityDetail') {
    return <CommunityDetailPage key={route.postId} postId={route.postId} />;
  }

  if (route.type === 'profileActivity') {
    return <ProfileActivityPage key={route.handle ?? 'me'} handle={route.handle} />;
  }

  if (route.type === 'profile') {
    return <ProfilePage key={route.handle ?? 'me'} handle={route.handle} />;
  }

  if (route.type === 'problems') {
    return <HomePage />;
  }

  return <DashboardPage />;
}
