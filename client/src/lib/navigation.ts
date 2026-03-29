export const PROBLEMS_PATH = '/problems';
export const RANKING_PATH = '/ranking';
export const COMMUNITY_PATH = '/community';
export const PROFILE_PATH = '/profile';
export const LANDING_SIGNUP_PATH = '/#signup';
export const DEFAULT_PROBLEM_PATH = '/problems/p-101';

interface NavigateOptions {
  replace?: boolean;
}

export function navigate(path: string, options: NavigateOptions = {}) {
  const method = options.replace ? 'replaceState' : 'pushState';

  window.history[method]({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}
