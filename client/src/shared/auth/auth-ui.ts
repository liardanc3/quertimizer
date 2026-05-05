import { getUiTextValue } from '@/shared/config/ui-text';

export type AuthSocialProvider = 'google' | 'github' | 'kakao';

export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const PASSWORD_RESET_CODE_PATTERN = /^[A-Z0-9]{6}$/;

export function hasRequiredPasswordFormat(value: string) {
  return value.length >= 8 && /[^A-Za-z0-9]/.test(value);
}

export function sanitizeVerificationCode(value: string) {
  return value.replace(/[^A-Za-z0-9]/g, '').toUpperCase().slice(0, 6);
}

export function getAuthSocialLoginErrorMessage(provider: string | null | undefined) {
  switch (provider?.toLowerCase()) {
    case 'google':
      return getUiTextValue('AUTH_GOOGLE_LOGIN_FAIL_MESSAGE', 'Google 로그인에 실패했습니다.');
    case 'github':
      return getUiTextValue('AUTH_GITHUB_LOGIN_FAIL_MESSAGE', 'Github 로그인에 실패했습니다.');
    case 'kakao':
      return getUiTextValue('AUTH_KAKAO_LOGIN_FAIL_MESSAGE', 'Kakao 로그인에 실패했습니다.');
    default:
      return getUiTextValue('AUTH_SOCIAL_LOGIN_FAIL_MESSAGE', '소셜 로그인에 실패했습니다.');
  }
}
