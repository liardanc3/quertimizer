import {
  ADMIN_PATH,
  COMMUNITY_PATH,
  COMMUNITY_WRITE_PATH,
  DASHBOARD_PATH,
  FAVORITES_PATH,
  GUIDE_PATH,
  PROFILE_ACTIVITY_PATH,
  PROFILE_PATH,
  PROBLEMS_PATH,
  RANKING_PATH,
  SUBMIT_HISTORY_PATH,
} from '../lib/navigation';

export interface ProblemRoute {
  type: 'problem';
  problemId: string;
}

export interface HomeRoute {
  type: 'home';
}

export interface DashboardRoute {
  type: 'dashboard';
}

export interface ProblemsRoute {
  type: 'problems';
}

export interface RankingRoute {
  type: 'ranking';
}

export interface SubmitHistoryRoute {
  type: 'submitHistory';
}

export interface FavoritesRoute {
  type: 'favorites';
}

export interface CommunityRoute {
  type: 'community';
}

export interface GuideRoute {
  type: 'guide';
}

export interface AdminRoute {
  type: 'admin';
}

export interface CommunityDetailRoute {
  type: 'communityDetail';
  postId: string;
}

export interface CommunityWriteRoute {
  type: 'communityWrite';
}

export interface CommunityEditRoute {
  type: 'communityEdit';
  postId: string;
}

export interface ProfileRoute {
  type: 'profile';
  handle?: string;
}

export interface ProfileActivityRoute {
  type: 'profileActivity';
  handle?: string;
}

export interface NotFoundRoute {
  type: 'notFound';
}

export type AppRoute =
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
  | ProblemRoute
  | NotFoundRoute;

const routePatterns = {
  communityEdit: /^\/community\/([a-zA-Z0-9-]+)\/edit$/,
  communityDetail: /^\/community\/(?!write$)([a-zA-Z0-9-]+)$/,
  profileActivity: /^\/profile\/([\w-]+)\/activity$/,
  profile: /^\/profile\/([\w-]+)$/,
  problem: /^\/problems\/([a-zA-Z0-9-]+)$/,
};

function normalizePathname(pathname: string) {
  return pathname !== '/' ? pathname.replace(/\/+$/, '') : pathname;
}

function parseDynamicRoute(pathname: string): AppRoute | null {
  const communityEditMatch = pathname.match(routePatterns.communityEdit);
  if (communityEditMatch) {
    return { type: 'communityEdit', postId: decodeURIComponent(communityEditMatch[1]) };
  }

  const communityPostMatch = pathname.match(routePatterns.communityDetail);
  if (communityPostMatch) {
    return { type: 'communityDetail', postId: decodeURIComponent(communityPostMatch[1]) };
  }

  const profileActivityMatch = pathname.match(routePatterns.profileActivity);
  if (profileActivityMatch) {
    return { type: 'profileActivity', handle: decodeURIComponent(profileActivityMatch[1]) };
  }

  const profileMatch = pathname.match(routePatterns.profile);
  if (profileMatch) {
    return { type: 'profile', handle: decodeURIComponent(profileMatch[1]) };
  }

  const problemMatch = pathname.match(routePatterns.problem);
  if (problemMatch) {
    return { type: 'problem', problemId: problemMatch[1] };
  }

  return null;
}

export function parseRoute(pathname: string): AppRoute {
  const normalizedPathname = normalizePathname(pathname);

  switch (normalizedPathname) {
    case '/':
      return { type: 'home' };
    case DASHBOARD_PATH:
      return { type: 'dashboard' };
    case PROBLEMS_PATH:
      return { type: 'problems' };
    case `${PROBLEMS_PATH}/create`:
      return { type: 'admin' };
    case RANKING_PATH:
      return { type: 'ranking' };
    case SUBMIT_HISTORY_PATH:
      return { type: 'submitHistory' };
    case FAVORITES_PATH:
      return { type: 'favorites' };
    case COMMUNITY_PATH:
      return { type: 'community' };
    case GUIDE_PATH:
      return { type: 'guide' };
    case ADMIN_PATH:
      return { type: 'admin' };
    case COMMUNITY_WRITE_PATH:
      return { type: 'communityWrite' };
    case PROFILE_PATH:
      return { type: 'profile' };
    case PROFILE_ACTIVITY_PATH:
      return { type: 'profileActivity' };
    default:
      return parseDynamicRoute(normalizedPathname) ?? { type: 'notFound' };
  }
}

export function routeNeedsSession(route: AppRoute) {
  return route.type === 'admin'
    || route.type === 'communityWrite'
    || route.type === 'communityEdit'
    || (route.type === 'profile' && route.handle == null)
    || (route.type === 'profileActivity' && route.handle == null);
}
