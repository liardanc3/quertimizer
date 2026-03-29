import { useSyncExternalStore } from 'react';
import CommunityPage from './pages/CommunityPage';
import HomePage from './pages/HomePage';
import ProfilePage from './pages/ProfilePage';
import ProblemSolvePage from './pages/ProblemSolvePage';
import PublicHomePage from './pages/PublicHomePage';
import RankingPage from './pages/RankingPage';
import { useMockSession } from './lib/session';

interface ProblemRoute {
  type: 'problem';
  problemId: string;
}

interface HomeRoute {
  type: 'home';
}

interface ProblemsRoute {
  type: 'problems';
}

interface RankingRoute {
  type: 'ranking';
}

interface CommunityRoute {
  type: 'community';
}

interface ProfileRoute {
  type: 'profile';
  handle?: string;
}

type AppRoute = HomeRoute | ProblemsRoute | RankingRoute | CommunityRoute | ProfileRoute | ProblemRoute;

function subscribe(callback: () => void) {
  window.addEventListener('popstate', callback);
  return () => window.removeEventListener('popstate', callback);
}

function getSnapshot() {
  return window.location.pathname;
}

function parseRoute(pathname: string): AppRoute {
  if (pathname === '/') {
    return { type: 'home' };
  }

  if (pathname === '/problems') {
    return { type: 'problems' };
  }

  if (pathname === '/ranking') {
    return { type: 'ranking' };
  }

  if (pathname === '/community') {
    return { type: 'community' };
  }

  if (pathname === '/profile') {
    return { type: 'profile' };
  }

  const profileMatch = pathname.match(/^\/profile\/([\w-]+)$/);
  if (profileMatch) {
    return { type: 'profile', handle: decodeURIComponent(profileMatch[1]) };
  }

  const problemMatch = pathname.match(/^\/problems\/([a-zA-Z0-9-]+)$/);
  if (problemMatch) {
    return { type: 'problem', problemId: problemMatch[1] };
  }

  return { type: 'home' };
}

export default function AppRouter() {
  const pathname = useSyncExternalStore(subscribe, getSnapshot, () => '/');
  const route = parseRoute(pathname);
  const { isAuthenticated } = useMockSession();

  if (route.type === 'problem') {
    return <ProblemSolvePage key={route.problemId} problemId={route.problemId} />;
  }

  if (route.type === 'ranking') {
    return <RankingPage />;
  }

  if (route.type === 'community') {
    return <CommunityPage />;
  }

  if (route.type === 'profile') {
    return <ProfilePage key={route.handle ?? 'me'} handle={route.handle} />;
  }

  if (route.type === 'problems') {
    return <HomePage />;
  }

  return isAuthenticated ? <HomePage /> : <PublicHomePage />;
}
