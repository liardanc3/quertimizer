import { useSyncExternalStore } from 'react';
import HomePage from './pages/HomePage';
import ProblemSolvePage from './pages/ProblemSolvePage';

interface ProblemRoute {
  type: 'problem';
  problemId: string;
}

interface HomeRoute {
  type: 'home';
}

type AppRoute = HomeRoute | ProblemRoute;

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

  const problemMatch = pathname.match(/^\/problems\/([a-zA-Z0-9-]+)$/);
  if (problemMatch) {
    return { type: 'problem', problemId: problemMatch[1] };
  }

  return { type: 'home' };
}

export default function AppRouter() {
  const pathname = useSyncExternalStore(subscribe, getSnapshot, () => '/');
  const route = parseRoute(pathname);

  if (route.type === 'problem') {
    return <ProblemSolvePage problemId={route.problemId} />;
  }

  return <HomePage />;
}
