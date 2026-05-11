export const PROBLEMS_PATH = '/problems';
export const DASHBOARD_PATH = '/dashboard';
export const PROBLEM_CREATE_PATH = '/problems/create';
export const SUBMIT_HISTORY_PATH = '/submissions';
export const FAVORITES_PATH = '/favorites';
export const RANKING_PATH = '/ranking';
export const COMMUNITY_PATH = '/community';
export const ADMIN_PATH = '/admin';
export const COMMUNITY_WRITE_PATH = '/community/write';
export const PROFILE_PATH = '/profile';
export const PROFILE_ACTIVITY_PATH = '/profile/activity';
export const LANDING_SIGNUP_PATH = '/#signup';
export const LANDING_SETUP_HANDLE_PATH = '/#setup-handle';
export const LANDING_RESET_PASSWORD_PATH = '/#reset-password';
export const DEFAULT_PROBLEM_PATH = PROBLEMS_PATH;

interface NavigateOptions {
  replace?: boolean;
  state?: unknown;
}

export function getProfilePath(handle?: string) {
  if (!handle) {
    return PROFILE_PATH;
  }

  return `${PROFILE_PATH}/${encodeURIComponent(handle)}`;
}

export function getProfileActivityPath(handle?: string, tab?: 'posts' | 'comments' | 'likes') {
  const basePath = handle ? `${PROFILE_PATH}/${encodeURIComponent(handle)}/activity` : PROFILE_ACTIVITY_PATH;

  if (!tab) {
    return basePath;
  }

  return `${basePath}?tab=${encodeURIComponent(tab)}`;
}

export function getCommunityPostPath(postId: string) {
  return `${COMMUNITY_PATH}/${encodeURIComponent(postId)}`;
}

export function getCommunityPostEditPath(postId: string) {
  return `${COMMUNITY_PATH}/${encodeURIComponent(postId)}/edit`;
}

export function navigate(path: string, options: NavigateOptions = {}) {
  const method = options.replace ? 'replaceState' : 'pushState';

  window.history[method](options.state ?? {}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

export function subscribeLocation(callback: () => void) {
  window.addEventListener('popstate', callback);
  return () => window.removeEventListener('popstate', callback);
}

export function getLocationSearchSnapshot() {
  return window.location.search;
}
