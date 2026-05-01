export const SOCIAL_LOGIN_SUCCESS_MESSAGE = 'quertimizer-social-login-success';
export const SOCIAL_LOGIN_ERROR_MESSAGE = 'quertimizer-social-login-error';

const TRUSTED_PRODUCTION_ORIGINS = new Set([
  'https://quertimizer.com',
  'https://www.quertimizer.com',
]);

export function hasSocialLoginCallbackSearch(search: string) {
  const params = new URLSearchParams(search);
  return params.has('socialLoginSuccess') || params.has('socialLoginError');
}

export function isTrustedSocialLoginCallbackOrigin(origin: string) {
  if (typeof window !== 'undefined' && origin === window.location.origin) {
    return true;
  }

  return TRUSTED_PRODUCTION_ORIGINS.has(origin);
}

