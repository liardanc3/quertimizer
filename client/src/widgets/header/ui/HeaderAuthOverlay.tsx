import { useEffect, useRef, useState, type FormEvent, type RefObject } from 'react';
import {
  AuthApiError,
  RecoveryApiError,
  SignupApiError,
  checkDuplicateEmail,
  fetchSessionMe,
  getApiBaseUrl,
  login,
  resetPassword,
  sendPasswordResetCode,
  sendSignupVerificationCode,
  signup,
  verifyPasswordResetCode,
  verifySignupVerificationCode,
} from '@/shared/api/auth-api';
import { completeAuthentication } from '@/shared/auth/auth-session';
import {
  EMAIL_PATTERN,
  PASSWORD_RESET_CODE_PATTERN,
  getAuthSocialLoginErrorMessage,
  hasRequiredPasswordFormat,
  sanitizeVerificationCode,
  type AuthSocialProvider,
} from '@/shared/auth/auth-ui';
import {
  SOCIAL_LOGIN_ERROR_MESSAGE,
  SOCIAL_LOGIN_SUCCESS_MESSAGE,
  isTrustedSocialLoginCallbackOrigin,
} from '@/shared/auth/social-login-callback';
import { useUiText } from '@/shared/config/ui-text';
import logoImage from '@/shared/assets/logo.png';
import './HeaderAuthOverlay.css';

type HeaderAuthOverlayMode = 'login' | 'signup' | 'reset-password';
type HeaderAuthSocialProvider = AuthSocialProvider;
type SignupEmailCheckStatus = 'idle' | 'checking' | 'available' | 'duplicated';

interface HeaderAuthOverlayProps {
  description?: string | null;
  onClose: () => void;
  onAuthenticated: () => void;
  disableBackdropBlur?: boolean;
  hideModalBorder?: boolean;
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true" focusable="false">
      <path d="M5 5l10 10M15 5L5 15" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
    </svg>
  );
}

function GithubMarkIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        fill="currentColor"
        d="M12 1.5a10.5 10.5 0 0 0-3.32 20.46c.53.1.72-.23.72-.51v-1.78c-2.93.64-3.55-1.24-3.55-1.24-.48-1.22-1.18-1.55-1.18-1.55-.96-.66.07-.64.07-.64 1.06.08 1.62 1.09 1.62 1.09.95 1.62 2.48 1.15 3.08.88.09-.68.37-1.15.67-1.42-2.34-.27-4.8-1.17-4.8-5.22 0-1.15.41-2.1 1.08-2.84-.11-.27-.47-1.38.1-2.88 0 0 .89-.28 2.91 1.08a10.02 10.02 0 0 1 5.3 0c2.02-1.36 2.91-1.08 2.91-1.08.57 1.5.21 2.61.1 2.88.67.74 1.08 1.69 1.08 2.84 0 4.06-2.46 4.94-4.81 5.21.38.33.72.98.72 1.98v2.93c0 .28.19.62.73.51A10.5 10.5 0 0 0 12 1.5Z"
      />
    </svg>
  );
}

function GoogleMarkIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        fill="#4285F4"
        d="M23.52 12.27c0-.79-.07-1.55-.2-2.27H12v4.3h6.47a5.54 5.54 0 0 1-2.4 3.63v3.02h3.88c2.27-2.08 3.57-5.15 3.57-8.68Z"
      />
      <path
        fill="#34A853"
        d="M12 24c3.24 0 5.96-1.07 7.95-2.9l-3.88-3.02c-1.08.72-2.46 1.15-4.07 1.15-3.13 0-5.78-2.11-6.72-4.95H1.27v3.12A12 12 0 0 0 12 24Z"
      />
      <path
        fill="#FBBC05"
        d="M5.28 14.28A7.2 7.2 0 0 1 4.9 12c0-.79.14-1.56.38-2.28V6.6H1.27a12 12 0 0 0 0 10.8l4.01-3.12Z"
      />
      <path
        fill="#EA4335"
        d="M12 4.77c1.76 0 3.34.61 4.58 1.8l3.43-3.43C17.95 1.19 15.23 0 12 0A12 12 0 0 0 1.27 6.6l4.01 3.12c.94-2.84 3.59-4.95 6.72-4.95Z"
      />
    </svg>
  );
}

function KakaoMarkIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <circle cx="12" cy="12" r="11" fill="#FEE500" />
      <path
        fill="#3B2727"
        d="M12.1 6.15c-4.02 0-7.28 2.52-7.28 5.63 0 1.84 1.13 3.48 2.88 4.5l-.78 2.55 3.14-2.08c.65.14 1.33.22 2.04.22 4.02 0 7.28-2.52 7.28-5.62 0-3.11-3.26-5.2-7.28-5.2Z"
      />
    </svg>
  );
}

function EmailMarkIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        d="M4.5 6.75h15a1.5 1.5 0 0 1 1.5 1.5v7.5a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 15.75v-7.5a1.5 1.5 0 0 1 1.5-1.5Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinejoin="round"
      />
      <path
        d="m4 8 8 5.5L20 8"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export default function HeaderAuthOverlay({
  description = null,
  onClose,
  onAuthenticated,
  disableBackdropBlur = false,
  hideModalBorder = false,
}: HeaderAuthOverlayProps) {
  const { text } = useUiText();
  const [mode, setMode] = useState<HeaderAuthOverlayMode>('login');
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [loginErrors, setLoginErrors] = useState<string[]>([]);
  const [isLoginSubmitting, setIsLoginSubmitting] = useState(false);
  const [isSocialLoginSubmitting, setIsSocialLoginSubmitting] = useState(false);
  const [signupEmail, setSignupEmail] = useState('');
  const [signupCode, setSignupCode] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [signupPasswordConfirm, setSignupPasswordConfirm] = useState('');
  const [signupErrors, setSignupErrors] = useState<string[]>([]);
  const [signupStatusMessage, setSignupStatusMessage] = useState<string | null>(null);
  const [isSignupSubmitting, setIsSignupSubmitting] = useState(false);
  const [isSendingSignupCode, setIsSendingSignupCode] = useState(false);
  const [isVerifyingSignupCode, setIsVerifyingSignupCode] = useState(false);
  const [isSignupCodeSent, setIsSignupCodeSent] = useState(false);
  const [isSignupCodeVerified, setIsSignupCodeVerified] = useState(false);
  const [signupEmailCheckStatus, setSignupEmailCheckStatus] = useState<SignupEmailCheckStatus>('idle');
  const [signupEmailCheckReason, setSignupEmailCheckReason] = useState<string | null>(null);
  const [signupEmailLastCheckedValue, setSignupEmailLastCheckedValue] = useState('');
  const [resetEmail, setResetEmail] = useState('');
  const [resetCode, setResetCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [resetErrors, setResetErrors] = useState<string[]>([]);
  const [resetStatusMessage, setResetStatusMessage] = useState<string | null>(null);
  const [isSendingResetCode, setIsSendingResetCode] = useState(false);
  const [isVerifyingResetCode, setIsVerifyingResetCode] = useState(false);
  const [isResettingPassword, setIsResettingPassword] = useState(false);
  const [isResetCodeSent, setIsResetCodeSent] = useState(false);
  const [isResetCodeVerified, setIsResetCodeVerified] = useState(false);
  const socialLoginPopupPollIdRef = useRef<number | null>(null);
  const returnToLoginTimeoutRef = useRef<number | null>(null);
  const signupEmailCheckSequenceRef = useRef(0);
  const signupCodeInputRef = useRef<HTMLInputElement | null>(null);
  const signupPasswordInputRef = useRef<HTMLInputElement | null>(null);
  const signupPasswordConfirmInputRef = useRef<HTMLInputElement | null>(null);
  const resetCodeInputRef = useRef<HTMLInputElement | null>(null);
  const resetPasswordInputRef = useRef<HTMLInputElement | null>(null);
  const resetPasswordConfirmInputRef = useRef<HTMLInputElement | null>(null);

  const normalizedLoginEmail = loginEmail.trim();
  const normalizedSignupEmail = signupEmail.trim();
  const normalizedSignupCode = signupCode.trim().toUpperCase();
  const normalizedResetEmail = resetEmail.trim();
  const normalizedResetCode = resetCode.trim().toUpperCase();
  const isLoginReady = normalizedLoginEmail !== '' && loginPassword.trim() !== '';
  const isSignupEmailValid = EMAIL_PATTERN.test(normalizedSignupEmail);
  const isSignupCodeValid = PASSWORD_RESET_CODE_PATTERN.test(normalizedSignupCode);
  const isSignupPasswordValid = hasRequiredPasswordFormat(signupPassword);
  const isSignupPasswordConfirmValid = signupPasswordConfirm !== '' && signupPasswordConfirm === signupPassword;
  const isSignupReady =
    isSignupEmailValid &&
    isSignupCodeVerified &&
    isSignupPasswordValid &&
    isSignupPasswordConfirmValid &&
    signupEmailCheckStatus !== 'checking';
  const isResetEmailValid = EMAIL_PATTERN.test(normalizedResetEmail);
  const isResetCodeValid = PASSWORD_RESET_CODE_PATTERN.test(normalizedResetCode);
  const isResetPasswordValid = hasRequiredPasswordFormat(newPassword);
  const isResetPasswordConfirmValid = newPasswordConfirm !== '' && newPasswordConfirm === newPassword;
  const authEmailHint = text('AUTH_EMAIL_HINT', '올바른 이메일 형식으로 입력해 주세요.');
  const authEmailCheckingMessage = text('AUTH_EMAIL_CHECKING_MESSAGE', '이메일 사용 가능 여부를 확인하는 중입니다.');
  const authEmailAvailableMessage = text('AUTH_EMAIL_AVAILABLE_MESSAGE', '사용 가능한 이메일입니다.');
  const authEmailDuplicatedMessage = text('AUTH_EMAIL_DUPLICATED_MESSAGE', '이미 사용 중인 이메일입니다.');
  const authCodeHint = text('AUTH_CODE_HINT', '이메일로 받은 인증코드 6자를 입력해 주세요.');
  const authCodeSentMessage = text('AUTH_CODE_SENT_MESSAGE', '인증 코드를 전송했습니다. 5분 이내에 입력해 주세요.');
  const authCodeVerifiedMessage = text('AUTH_CODE_VERIFIED_MESSAGE', '인증 코드가 확인되었습니다. 비밀번호를 입력해 주세요.');
  const authPasswordHint = text('AUTH_PASSWORD_HINT', '특수문자를 포함해 8자 이상 입력해 주세요.');
  const authPasswordConfirmHint = text('AUTH_PASSWORD_CONFIRM_HINT', '비밀번호를 다시 입력해 주세요.');
  const authResetCodeVerifiedMessage = text('AUTH_RESET_CODE_VERIFIED_MESSAGE', '인증 코드가 확인되었습니다. 새 비밀번호를 입력해 주세요.');
  const authResetPasswordChangedMessage = text('AUTH_RESET_PASSWORD_CHANGED_MESSAGE', '비밀번호가 변경되었습니다. 다시 로그인해 주세요.');
  const resetCodeSentStatusMessage = resetStatusMessage === authCodeSentMessage ? resetStatusMessage : null;
  const resetCodeVerifiedStatusMessage = resetStatusMessage === authResetCodeVerifiedMessage ? resetStatusMessage : null;
  const resetPasswordChangedStatusMessage = resetStatusMessage === authResetPasswordChangedMessage ? resetStatusMessage : null;
  const signupCodeSentStatusMessage = signupStatusMessage === authCodeSentMessage ? signupStatusMessage : null;
  const signupCodeVerifiedStatusMessage = signupStatusMessage === authCodeVerifiedMessage ? signupStatusMessage : null;
  const signupEmailHintMessage =
    signupCodeSentStatusMessage ??
    (normalizedSignupEmail === ''
      ? authEmailHint
      : !isSignupEmailValid
        ? authEmailHint
        : signupEmailCheckStatus === 'checking'
          ? authEmailCheckingMessage
          : signupEmailLastCheckedValue === normalizedSignupEmail && signupEmailCheckStatus === 'duplicated'
            ? (signupEmailCheckReason ?? authEmailDuplicatedMessage)
            : signupEmailLastCheckedValue === normalizedSignupEmail && signupEmailCheckStatus === 'available'
              ? authEmailAvailableMessage
              : authEmailHint);
  const hasSignupEmailError =
    normalizedSignupEmail !== '' &&
    (!isSignupEmailValid || (signupEmailLastCheckedValue === normalizedSignupEmail && signupEmailCheckStatus === 'duplicated'));
  const hasSignupEmailSuccess =
    normalizedSignupEmail !== '' &&
    !hasSignupEmailError &&
    ((signupEmailLastCheckedValue === normalizedSignupEmail && signupEmailCheckStatus === 'available') || signupCodeSentStatusMessage != null);

  const focusNextInput = (nextInputRef: RefObject<HTMLInputElement | null>) => {
    window.requestAnimationFrame(() => {
      nextInputRef.current?.focus();
    });
  };

  useEffect(() => {
    document.body.classList.add('header-auth-locked');

    return () => {
      document.body.classList.remove('header-auth-locked');

      if (socialLoginPopupPollIdRef.current != null) {
        window.clearInterval(socialLoginPopupPollIdRef.current);
      }

      if (returnToLoginTimeoutRef.current != null) {
        window.clearTimeout(returnToLoginTimeoutRef.current);
      }
    };
  }, []);

  const startSocialLogin = (provider: HeaderAuthSocialProvider) => {
    if (typeof window === 'undefined' || isSocialLoginSubmitting) {
      return;
    }

    if (socialLoginPopupPollIdRef.current != null) {
      window.clearInterval(socialLoginPopupPollIdRef.current);
      socialLoginPopupPollIdRef.current = null;
    }

    setLoginErrors([]);
    setIsSocialLoginSubmitting(true);

    const popupWidth = 520;
    const popupHeight = 760;
    const popupLeft = Math.max(0, Math.round(window.screenX + (window.outerWidth - popupWidth) / 2));
    const popupTop = Math.max(0, Math.round(window.screenY + (window.outerHeight - popupHeight) / 2));
    const popup = window.open(
      `${getApiBaseUrl()}/oauth2/authorization/${provider}`,
      `quertimizer-header-social-login-${provider}`,
      `popup=yes,width=${popupWidth},height=${popupHeight},left=${popupLeft},top=${popupTop},resizable=yes,scrollbars=yes`,
    );

    if (!popup) {
      setIsSocialLoginSubmitting(false);
      setLoginErrors([text('AUTH_POPUP_BLOCKED_MESSAGE', '팝업이 차단되어 소셜 로그인을 진행할 수 없습니다.')]);
      return;
    }

    popup.focus();
    let isChecking = false;
    let isCompleting = false;
    let didClosedSessionCheck = false;
    let removeMessageListener = () => {};

    const stopPolling = () => {
      if (socialLoginPopupPollIdRef.current != null) {
        window.clearInterval(socialLoginPopupPollIdRef.current);
        socialLoginPopupPollIdRef.current = null;
      }

      removeMessageListener();
      setIsSocialLoginSubmitting(false);
    };

    const completePopupAuthentication = async (shouldClosePopup: boolean) => {
      if (isCompleting) {
        return;
      }

      isCompleting = true;

      try {
        const session = await fetchSessionMe();
        if (!session.authenticated) {
          isCompleting = false;
          return;
        }

        await completeAuthentication(session);
        if (shouldClosePopup && !popup.closed) {
          popup.close();
        }
        stopPolling();
        onAuthenticated();
      } catch {
        isCompleting = false;
      }
    };

    const handlePopupMessage = (event: MessageEvent) => {
      if (!isTrustedSocialLoginCallbackOrigin(event.origin) || event.data == null || typeof event.data !== 'object') {
        return;
      }

      const message = event.data as { type?: string; provider?: HeaderAuthSocialProvider | 'oauth2' | null };
      if (message.type !== SOCIAL_LOGIN_SUCCESS_MESSAGE && message.type !== SOCIAL_LOGIN_ERROR_MESSAGE) {
        return;
      }

      void (async () => {
        if (message.type === SOCIAL_LOGIN_ERROR_MESSAGE) {
          popup.close();
          stopPolling();
          setLoginErrors([getAuthSocialLoginErrorMessage(message.provider ?? null)]);
          return;
        }

        await completePopupAuthentication(true);
      })();
    };

    window.addEventListener('message', handlePopupMessage);
    removeMessageListener = () => {
      window.removeEventListener('message', handlePopupMessage);
      removeMessageListener = () => {};
    };

    const pollPopupState = async () => {
      if (isChecking) {
        return;
      }

      isChecking = true;

      try {
        if (popup.closed) {
          if (!didClosedSessionCheck) {
            didClosedSessionCheck = true;
            await completePopupAuthentication(false);
          }
          stopPolling();
          return;
        }

        try {
          const popupUrl = new URL(popup.location.href);
          if (popupUrl.origin === window.location.origin) {
            const socialLoginError = popupUrl.searchParams.get('socialLoginError') as HeaderAuthSocialProvider | 'oauth2' | null;
            if (socialLoginError != null) {
              popup.close();
              stopPolling();
              setLoginErrors([getAuthSocialLoginErrorMessage(socialLoginError)]);
              return;
            }

            if (popupUrl.searchParams.has('socialLoginSuccess')) {
              await completePopupAuthentication(true);
              return;
            }
          }
        } catch {
          // 크로스 오리진 팝업은 location 접근을 허용하지 않음
        }
      } catch {
        // 팝업 상태 확인 실패는 다음 polling 주기에서 다시 확인
      } finally {
        isChecking = false;
      }
    };

    socialLoginPopupPollIdRef.current = window.setInterval(() => {
      void pollPopupState();
    }, 1500);
    void pollPopupState();
  };

  const resetSignupEmailCheck = () => {
    setSignupEmailCheckStatus('idle');
    setSignupEmailCheckReason(null);
    setSignupEmailLastCheckedValue('');
  };

  const resetSignupVerification = () => {
    setSignupCode('');
    setSignupStatusMessage(null);
    setIsSignupCodeSent(false);
    setIsSignupCodeVerified(false);
  };

  const applySignupErrorReasons = (reasons: string[]) => {
    const nextErrors: string[] = [];

    for (const reason of reasons) {
      if (reason.includes('이메일') && (reason.includes('중복') || reason.includes('사용 중'))) {
        setSignupEmailCheckStatus('duplicated');
        setSignupEmailCheckReason(reason);
        setSignupEmailLastCheckedValue(normalizedSignupEmail);
        continue;
      }

      nextErrors.push(reason);
    }

    setSignupErrors(nextErrors);
  };

  const checkSignupEmailDuplication = async () => {
    if (normalizedSignupEmail === '') {
      resetSignupEmailCheck();
      return false;
    }

    if (!isSignupEmailValid) {
      resetSignupEmailCheck();
      return false;
    }

    if (signupEmailLastCheckedValue === normalizedSignupEmail) {
      return signupEmailCheckStatus === 'available';
    }

    const requestSequence = signupEmailCheckSequenceRef.current + 1;
    signupEmailCheckSequenceRef.current = requestSequence;
    setSignupEmailCheckStatus('checking');
    setSignupEmailCheckReason(null);
    setSignupErrors([]);

    try {
      const result = await checkDuplicateEmail(normalizedSignupEmail);

      if (requestSequence !== signupEmailCheckSequenceRef.current) {
        return false;
      }

      setSignupEmailCheckStatus(result.available ? 'available' : 'duplicated');
      setSignupEmailCheckReason(result.reason);
      setSignupEmailLastCheckedValue(normalizedSignupEmail);
      return result.available;
    } catch (error) {
      if (requestSequence !== signupEmailCheckSequenceRef.current) {
        return false;
      }

      setSignupEmailCheckStatus('idle');
      setSignupEmailLastCheckedValue(normalizedSignupEmail);

      if (error instanceof SignupApiError) {
        setSignupErrors(error.reasons);
        return false;
      }

      setSignupErrors([error instanceof Error ? error.message : text('AUTH_EMAIL_DUPLICATE_CHECK_FAIL_MESSAGE', '이메일 중복 확인 중 오류가 발생했습니다.')]);
      return false;
    }
  };

  const handleLoginSubmit = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();

    if (!isLoginReady) {
      return;
    }

    try {
      setIsLoginSubmitting(true);
      setLoginErrors([]);

      const session = await login({
        email: normalizedLoginEmail,
        password: loginPassword,
      });

      await completeAuthentication(session);
      if (!session.authenticated) {
        setLoginErrors([text('AUTH_LOGIN_FAIL_MESSAGE', '로그인에 실패했습니다.')]);
        return;
      }

      onAuthenticated();
    } catch (error) {
      if (error instanceof AuthApiError) {
        setLoginErrors(error.reasons);
        return;
      }

      setLoginErrors([error instanceof Error ? error.message : text('AUTH_LOGIN_ERROR_MESSAGE', '로그인 중 오류가 발생했습니다.')]);
    } finally {
      setIsLoginSubmitting(false);
    }
  };

  const handleSendSignupCode = async () => {
    if (!isSignupEmailValid || isSendingSignupCode) {
      return false;
    }

    try {
      setIsSendingSignupCode(true);
      setSignupErrors([]);
      setSignupStatusMessage(null);

      const isEmailAvailable = await checkSignupEmailDuplication();
      if (!isEmailAvailable) {
        return false;
      }

      await sendSignupVerificationCode({ email: normalizedSignupEmail });
      setSignupCode('');
      setIsSignupCodeSent(true);
      setIsSignupCodeVerified(false);
      setSignupStatusMessage(authCodeSentMessage);
      return true;
    } catch (error) {
      if (error instanceof SignupApiError) {
        applySignupErrorReasons(error.reasons);
        return false;
      }

      setSignupErrors([error instanceof Error ? error.message : text('AUTH_CODE_SEND_FAIL_MESSAGE', '인증 코드 전송 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsSendingSignupCode(false);
    }
  };

  const handleVerifySignupCode = async () => {
    if (!isSignupCodeSent || !isSignupCodeValid || isVerifyingSignupCode) {
      return false;
    }

    try {
      setIsVerifyingSignupCode(true);
      setSignupErrors([]);
      setSignupStatusMessage(null);

      await verifySignupVerificationCode({
        email: normalizedSignupEmail,
        code: normalizedSignupCode,
      });
      setIsSignupCodeVerified(true);
      setSignupStatusMessage(authCodeVerifiedMessage);
      return true;
    } catch (error) {
      setIsSignupCodeVerified(false);
      if (error instanceof SignupApiError) {
        applySignupErrorReasons(error.reasons);
        return false;
      }

      setSignupErrors([error instanceof Error ? error.message : text('AUTH_CODE_VERIFY_FAIL_MESSAGE', '인증 코드 확인 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsVerifyingSignupCode(false);
    }
  };

  const handleSignupSubmit = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();

    if (!isSignupReady) {
      return false;
    }

    try {
      setIsSignupSubmitting(true);
      setSignupErrors([]);

      const isEmailAvailable = await checkSignupEmailDuplication();
      if (!isEmailAvailable) {
        return false;
      }

      await signup({
        email: normalizedSignupEmail,
        password: signupPassword,
        code: normalizedSignupCode,
      });

      const session = await fetchSessionMe();
      await completeAuthentication(session);

      if (!session.authenticated) {
        setSignupErrors([text('AUTH_SIGNUP_SESSION_FAIL_MESSAGE', '회원가입 후 세션을 확인하지 못했습니다.')]);
        return false;
      }

      onAuthenticated();
      return true;
    } catch (error) {
      if (error instanceof SignupApiError || error instanceof AuthApiError) {
        applySignupErrorReasons(error.reasons);
        return false;
      }

      setSignupErrors([error instanceof Error ? error.message : text('AUTH_SIGNUP_ERROR_MESSAGE', '회원가입 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsSignupSubmitting(false);
    }
  };

  const handleSendResetCode = async () => {
    if (!isResetEmailValid || isSendingResetCode) {
      return false;
    }

    try {
      setIsSendingResetCode(true);
      setResetErrors([]);
      setResetStatusMessage(null);
      await sendPasswordResetCode({ email: normalizedResetEmail });
      setIsResetCodeSent(true);
      setResetCode('');
      setIsResetCodeVerified(false);
      setNewPassword('');
      setNewPasswordConfirm('');
      setResetStatusMessage(authCodeSentMessage);
      return true;
    } catch (error) {
      if (error instanceof RecoveryApiError) {
        setResetErrors(error.reasons);
        return false;
      }

      setResetErrors([error instanceof Error ? error.message : text('AUTH_CODE_SEND_FAIL_MESSAGE', '인증 코드 전송 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsSendingResetCode(false);
    }
  };

  const handleVerifyResetCode = async () => {
    if (!isResetCodeSent || !isResetCodeValid || isVerifyingResetCode) {
      return false;
    }

    try {
      setIsVerifyingResetCode(true);
      setResetErrors([]);
      setResetStatusMessage(null);
      await verifyPasswordResetCode({
        email: normalizedResetEmail,
        code: normalizedResetCode,
      });
      setIsResetCodeVerified(true);
      setResetStatusMessage(authResetCodeVerifiedMessage);
      return true;
    } catch (error) {
      setIsResetCodeVerified(false);
      if (error instanceof RecoveryApiError) {
        setResetErrors(error.reasons);
        return false;
      }

      setResetErrors([error instanceof Error ? error.message : text('AUTH_CODE_VERIFY_FAIL_MESSAGE', '인증 코드 확인 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsVerifyingResetCode(false);
    }
  };

  const handleResetPassword = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();

    if (!isResetCodeVerified || !isResetPasswordValid || !isResetPasswordConfirmValid || isResettingPassword) {
      return false;
    }

    try {
      setIsResettingPassword(true);
      setResetErrors([]);
      setResetStatusMessage(null);
      await resetPassword({
        email: normalizedResetEmail,
        password: newPassword,
      });
      setResetStatusMessage(authResetPasswordChangedMessage);
      setNewPassword('');
      setNewPasswordConfirm('');
      returnToLoginTimeoutRef.current = window.setTimeout(() => {
        setMode('login');
        setResetErrors([]);
      }, 300);
      return true;
    } catch (error) {
      if (error instanceof RecoveryApiError) {
        setResetErrors(error.reasons);
        return false;
      }

      setResetErrors([error instanceof Error ? error.message : text('AUTH_PASSWORD_CHANGE_FAIL_MESSAGE', '비밀번호 변경 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsResettingPassword(false);
    }
  };

  const overlayTitle =
    mode === 'signup'
      ? text('AUTH_SIGNUP_TITLE', '이메일로 가입하기')
      : mode === 'reset-password'
        ? text('AUTH_RESET_TITLE', '비밀번호 찾기')
        : text('AUTH_LOGIN_TITLE', '로그인');
  const signupDescription = description?.trim().replace('로그인 후', '가입 후') ?? '';
  const overlayDescription =
    mode === 'signup'
      ? signupDescription
      : mode === 'reset-password'
        ? text('AUTH_RESET_DESCRIPTION', '인증 코드를 확인한 뒤 새 비밀번호를 설정합니다.')
        : description?.trim() ?? '';

  return (
    <div className="header-auth-overlay" role="presentation">
      <div className={`header-auth-overlay-backdrop ${disableBackdropBlur ? 'is-without-blur' : ''}`.trim()} />
      <section className={`header-auth-modal ${hideModalBorder ? 'is-borderless' : ''}`.trim()} role="dialog" aria-modal="true" aria-label={overlayTitle}>
        <button
          type="button"
          className="header-auth-modal-close"
          aria-label={text('AUTH_LOGIN_MODAL_CLOSE_LABEL', '로그인 팝업 닫기')}
          onClick={onClose}
        >
          <CloseIcon />
        </button>

        <div className="header-auth-modal-header">
          <div className="header-auth-modal-copy">
            <h2 className="header-auth-modal-title">{overlayTitle}</h2>
            {overlayDescription ? <p className="header-auth-modal-description">{overlayDescription}</p> : null}
          </div>
        </div>

        {mode === 'login' ? (
          <div className="header-auth-landing-body">
            <div className="minimal-auth-form header-auth-minimal-form">
              <div className="landing-auth-layout">
                <form
                  className="landing-login-panel"
                  aria-label={text('AUTH_LOGIN_FORM_LABEL', '로그인 입력')}
                  onSubmit={(event) => void handleLoginSubmit(event)}
                >
                  <div className="field-stack">
                    <label className="field-label" htmlFor="header-auth-email">
                      {text('AUTH_EMAIL_LABEL', '이메일')}
                    </label>
                    <input
                      id="header-auth-email"
                      type="email"
                      className="text-field"
                      autoComplete="email"
                      value={loginEmail}
                      onChange={(event) => {
                        setLoginEmail(event.target.value);
                        setLoginErrors([]);
                      }}
                      placeholder={text('AUTH_LOGIN_EMAIL_PLACEHOLDER', '이메일을 입력하세요')}
                      inputMode="email"
                    />
                  </div>

                  <div className="field-stack">
                    <label className="field-label" htmlFor="header-auth-password">
                      {text('AUTH_PASSWORD_LABEL', '비밀번호')}
                    </label>
                    <input
                      id="header-auth-password"
                      type="password"
                      className="text-field"
                      autoComplete="current-password"
                      value={loginPassword}
                      onChange={(event) => {
                        setLoginPassword(event.target.value);
                        setLoginErrors([]);
                      }}
                      onKeyDown={(event) => {
                        if (event.key !== 'Enter') {
                          return;
                        }

                        event.preventDefault();
                        void handleLoginSubmit();
                      }}
                      placeholder={text('AUTH_LOGIN_PASSWORD_PLACEHOLDER', '비밀번호를 입력하세요')}
                    />
                  </div>

                  {loginErrors.length > 0 ? (
                    <div className="signup-feedback-box" role="alert" aria-live="polite">
                      {loginErrors.map((reason) => (
                        <p key={reason} className="signup-feedback-message">
                          {reason}
                        </p>
                      ))}
                    </div>
                  ) : null}

                  <div className="auth-actions minimal">
                    <button
                      type="submit"
                      className="btn primary landing-login-submit"
                      disabled={!isLoginReady || isLoginSubmitting}
                    >
                      {isLoginSubmitting ? text('AUTH_LOGIN_IN_PROGRESS_ELLIPSIS', '로그인 중…') : text('AUTH_LOGIN_TITLE', '로그인')}
                    </button>
                  </div>

                  <button type="button" className="btn text landing-password-reset-link" onClick={() => setMode('reset-password')}>
                    {text('AUTH_FORGOT_PASSWORD_LINK', '비밀번호를 잊으셨나요?')}
                  </button>
                </form>

                <div className="landing-auth-divider" aria-hidden="true">
                  <span className="landing-auth-divider-line" />
                  <img className="landing-auth-divider-mark" src={logoImage} alt="" />
                  <span className="landing-auth-divider-line" />
                </div>

                <aside className="landing-access-panel" aria-label={text('AUTH_ACCOUNT_SUPPORT_LABEL', '계정 지원')}>
                  <div className="landing-access-group landing-access-group-social">
                    <button type="button" className="landing-access-card is-social" onClick={() => startSocialLogin('google')} disabled={isSocialLoginSubmitting}>
                      <span className="landing-access-card-icon" aria-hidden="true">
                        <GoogleMarkIcon />
                      </span>
                      <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_GOOGLE', 'Google로 계속하기')}</span>
                    </button>

                    <button type="button" className="landing-access-card is-social" onClick={() => startSocialLogin('github')} disabled={isSocialLoginSubmitting}>
                      <span className="landing-access-card-icon" aria-hidden="true">
                        <GithubMarkIcon />
                      </span>
                      <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_GITHUB', 'Github로 계속하기')}</span>
                    </button>

                    <button type="button" className="landing-access-card is-social" onClick={() => startSocialLogin('kakao')} disabled={isSocialLoginSubmitting}>
                      <span className="landing-access-card-icon" aria-hidden="true">
                        <KakaoMarkIcon />
                      </span>
                      <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_KAKAO', 'Kakao로 계속하기')}</span>
                    </button>
                  </div>

                  <div className="landing-access-group landing-access-group-support">
                    <button type="button" className="landing-access-card is-social is-email" onClick={() => setMode('signup')}>
                      <span className="landing-access-card-icon" aria-hidden="true">
                        <EmailMarkIcon />
                      </span>
                      <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_EMAIL', '이메일로 계속하기')}</span>
                    </button>
                  </div>
                </aside>
              </div>
            </div>
          </div>
        ) : mode === 'signup' ? (
          <form className="header-auth-signup-form" onSubmit={(event) => void handleSignupSubmit(event)}>
            <div className="field-stack header-auth-field-stack">
              <label className="field-label" htmlFor="header-signup-email">
                {text('AUTH_EMAIL_LABEL', '이메일')}
              </label>
              <div className="header-auth-inline-row">
                <input
                  id="header-signup-email"
                  type="email"
                  className="text-field"
                  onKeyDown={(event) => {
                    if (event.key !== 'Enter') {
                      return;
                    }

                    event.preventDefault();
                    void (async () => {
                      const isCodeSent = await handleSendSignupCode();
                      if (isCodeSent) {
                        focusNextInput(signupCodeInputRef);
                      }
                    })();
                  }}
                  autoComplete="email"
                  value={signupEmail}
                  onChange={(event) => {
                    setSignupEmail(event.target.value);
                    setSignupErrors([]);
                    resetSignupEmailCheck();
                    resetSignupVerification();
                  }}
                  onBlur={() => {
                    void checkSignupEmailDuplication();
                  }}
                  placeholder={text('AUTH_EMAIL_PLACEHOLDER_POLITE', '이메일을 입력해 주세요.')}
                  aria-invalid={hasSignupEmailError}
                />
                <button type="button" className="btn secondary" onClick={handleSendSignupCode} disabled={!isSignupEmailValid || isSendingSignupCode}>
                  {isSendingSignupCode ? text('COMMON_SENDING_LABEL', '전송 중') : text('AUTH_CODE_SEND_BUTTON', '코드 전송')}
                </button>
              </div>
              <p className={`header-auth-field-hint ${hasSignupEmailError ? 'is-error' : hasSignupEmailSuccess ? 'is-success' : ''}`}>
                {signupEmailHintMessage}
              </p>
            </div>

            <div className="field-stack header-auth-field-stack">
              <label className="field-label" htmlFor="header-signup-code">
                {text('AUTH_CODE_LABEL', '인증 코드')}
              </label>
              <div className="header-auth-inline-row">
                <input
                  id="header-signup-code"
                  type="text"
                  className="text-field"
                  ref={signupCodeInputRef}
                  value={signupCode}
                  onChange={(event) => {
                    setSignupCode(sanitizeVerificationCode(event.target.value));
                    setSignupErrors([]);
                    setSignupStatusMessage(null);
                    setIsSignupCodeVerified(false);
                  }}
                  onKeyDown={(event) => {
                    if (event.key !== 'Enter') {
                      return;
                    }

                    event.preventDefault();
                    void (async () => {
                      const isCodeVerified = await handleVerifySignupCode();
                      if (isCodeVerified) {
                        focusNextInput(signupPasswordInputRef);
                      }
                    })();
                  }}
                  placeholder={text('AUTH_CODE_PLACEHOLDER_POLITE', '이메일로 받은 6자리 코드를 입력해 주세요.')}
                  disabled={!isSignupCodeSent}
                />
                <button
                  type="button"
                  className="btn secondary"
                  onClick={handleVerifySignupCode}
                  disabled={!isSignupCodeSent || !isSignupCodeValid || isVerifyingSignupCode}
                >
                  {isVerifyingSignupCode ? text('COMMON_VERIFYING_LABEL', '확인 중') : text('AUTH_CODE_VERIFY_BUTTON', '코드 확인')}
                </button>
              </div>
              {signupCodeVerifiedStatusMessage ? <p className="header-auth-field-hint is-success">{signupCodeVerifiedStatusMessage}</p> : null}
              {!signupCodeVerifiedStatusMessage ? <p className="header-auth-field-hint">{authCodeHint}</p> : null}
            </div>

            <div className="field-stack header-auth-field-stack">
              <label className="field-label" htmlFor="header-signup-password">
                {text('AUTH_PASSWORD_LABEL', '비밀번호')}
              </label>
              <input
                id="header-signup-password"
                type="password"
                className="text-field"
                ref={signupPasswordInputRef}
                autoComplete="new-password"
                value={signupPassword}
                onChange={(event) => {
                  setSignupPassword(event.target.value);
                  setSignupErrors([]);
                }}
                onKeyDown={(event) => {
                  if (event.key !== 'Enter') {
                    return;
                  }

                  event.preventDefault();
                  focusNextInput(signupPasswordConfirmInputRef);
                }}
                placeholder={text('AUTH_PASSWORD_PLACEHOLDER_POLITE', '비밀번호를 입력해 주세요.')}
                aria-invalid={signupPassword.length > 0 && !isSignupPasswordValid}
              />
              <p className={`header-auth-field-hint ${signupPassword.length > 0 && !isSignupPasswordValid ? 'is-error' : signupPassword.length > 0 ? 'is-success' : ''}`}>
                {authPasswordHint}
              </p>
            </div>

            <div className="field-stack header-auth-field-stack">
              <label className="field-label" htmlFor="header-signup-password-confirm">
                {text('AUTH_PASSWORD_CONFIRM_LABEL', '비밀번호 확인')}
              </label>
              <input
                id="header-signup-password-confirm"
                type="password"
                className="text-field"
                ref={signupPasswordConfirmInputRef}
                autoComplete="new-password"
                value={signupPasswordConfirm}
                onChange={(event) => {
                  setSignupPasswordConfirm(event.target.value);
                  setSignupErrors([]);
                }}
                onKeyDown={(event) => {
                  if (event.key !== 'Enter') {
                    return;
                  }

                  event.preventDefault();
                  void handleSignupSubmit();
                }}
                placeholder={text('AUTH_PASSWORD_CONFIRM_PLACEHOLDER', '비밀번호를 다시 입력해 주세요.')}
                aria-invalid={signupPasswordConfirm.length > 0 && !isSignupPasswordConfirmValid}
              />
              <p className={`header-auth-field-hint ${signupPasswordConfirm.length > 0 && !isSignupPasswordConfirmValid ? 'is-error' : signupPasswordConfirm.length > 0 ? 'is-success' : ''}`}>
                {authPasswordConfirmHint}
              </p>
            </div>

            {signupErrors.length > 0 ? (
              <div className="header-auth-feedback is-error" role="alert">
                {signupErrors.map((reason) => (
                  <p key={reason}>{reason}</p>
                ))}
              </div>
            ) : null}

            <div className="header-auth-signup-actions">
              <button type="submit" className="btn primary" disabled={!isSignupReady || isSignupSubmitting}>
                {isSignupSubmitting ? text('AUTH_SIGNUP_PROGRESS_LABEL', '가입 중') : text('AUTH_SIGNUP_BUTTON', '가입하기')}
              </button>
              <button type="button" className="header-auth-reset-link" onClick={() => setMode('login')}>
                {text('AUTH_BACK_TO_LOGIN_BUTTON', '로그인으로 돌아가기')}
              </button>
            </div>
          </form>
        ) : (
          <form className="header-auth-reset-form" onSubmit={(event) => void handleResetPassword(event)}>
            <div className="field-stack header-auth-field-stack">
              <label className="field-label" htmlFor="header-reset-email">
                {text('AUTH_EMAIL_LABEL', '이메일')}
              </label>
              <div className="header-auth-inline-row">
                <input
                  id="header-reset-email"
                  type="email"
                  className="text-field"
                  onKeyDown={(event) => {
                    if (event.key !== 'Enter') {
                      return;
                    }

                    event.preventDefault();
                    void (async () => {
                      const isCodeSent = await handleSendResetCode();
                      if (isCodeSent) {
                        focusNextInput(resetCodeInputRef);
                      }
                    })();
                  }}
                  autoComplete="email"
                  value={resetEmail}
                  onChange={(event) => {
                    setResetEmail(event.target.value);
                    setResetErrors([]);
                    setResetStatusMessage(null);
                  }}
                  placeholder={text('AUTH_RESET_EMAIL_PLACEHOLDER', '가입한 이메일을 입력해 주세요.')}
                />
                <button type="button" className="btn secondary" onClick={handleSendResetCode} disabled={!isResetEmailValid || isSendingResetCode}>
                  {isSendingResetCode ? text('COMMON_SENDING_LABEL', '전송 중') : text('AUTH_CODE_SEND_BUTTON', '코드 전송')}
                </button>
              </div>
              {resetCodeSentStatusMessage ? <p className="header-auth-field-hint is-success">{resetCodeSentStatusMessage}</p> : null}
            </div>

            <div className="field-stack header-auth-field-stack">
              <label className="field-label" htmlFor="header-reset-code">
                {text('AUTH_CODE_LABEL', '인증 코드')}
              </label>
              <div className="header-auth-inline-row">
                <input
                  id="header-reset-code"
                  type="text"
                  className="text-field"
                  ref={resetCodeInputRef}
                  value={resetCode}
                  onChange={(event) => {
                    setResetCode(sanitizeVerificationCode(event.target.value));
                    setResetErrors([]);
                    setResetStatusMessage(null);
                  }}
                  onKeyDown={(event) => {
                    if (event.key !== 'Enter') {
                      return;
                    }

                    event.preventDefault();
                    void (async () => {
                      const isCodeVerified = await handleVerifyResetCode();
                      if (isCodeVerified) {
                        focusNextInput(resetPasswordInputRef);
                      }
                    })();
                  }}
                  placeholder={text('AUTH_CODE_PLACEHOLDER_POLITE', '이메일로 받은 6자리 코드를 입력해 주세요.')}
                />
                <button
                  type="button"
                  className="btn secondary"
                  onClick={handleVerifyResetCode}
                  disabled={!isResetCodeSent || !isResetCodeValid || isVerifyingResetCode}
                >
                  {isVerifyingResetCode ? text('COMMON_VERIFYING_LABEL', '확인 중') : text('AUTH_CODE_VERIFY_BUTTON', '코드 확인')}
                </button>
              </div>
              {resetCodeVerifiedStatusMessage ? <p className="header-auth-field-hint is-success">{resetCodeVerifiedStatusMessage}</p> : null}
            </div>

            <div className="field-stack header-auth-field-stack">
              <label className="field-label" htmlFor="header-reset-password">
                {text('AUTH_NEW_PASSWORD_LABEL', '새 비밀번호')}
              </label>
              <input
                id="header-reset-password"
                type="password"
                className="text-field"
                ref={resetPasswordInputRef}
                value={newPassword}
                onChange={(event) => {
                  setNewPassword(event.target.value);
                  setResetErrors([]);
                }}
                onKeyDown={(event) => {
                  if (event.key !== 'Enter') {
                    return;
                  }

                  event.preventDefault();
                  focusNextInput(resetPasswordConfirmInputRef);
                }}
                placeholder={text('AUTH_NEW_PASSWORD_PLACEHOLDER', '특수문자를 포함해 8자 이상 입력해 주세요.')}
                disabled={!isResetCodeVerified}
              />
              <p className={`header-auth-field-hint ${newPassword.length > 0 && !isResetPasswordValid ? 'is-error' : newPassword.length > 0 ? 'is-success' : ''}`}>
                {text('AUTH_PASSWORD_VALIDATION_MESSAGE', '비밀번호는 특수문자를 포함해 8자 이상이어야 합니다.')}
              </p>
            </div>

            <div className="field-stack header-auth-field-stack">
              <label className="field-label" htmlFor="header-reset-password-confirm">
                {text('AUTH_NEW_PASSWORD_CONFIRM_LABEL', '새 비밀번호 확인')}
              </label>
              <input
                id="header-reset-password-confirm"
                type="password"
                className="text-field"
                ref={resetPasswordConfirmInputRef}
                value={newPasswordConfirm}
                onChange={(event) => {
                  setNewPasswordConfirm(event.target.value);
                  setResetErrors([]);
                }}
                onKeyDown={(event) => {
                  if (event.key !== 'Enter') {
                    return;
                  }

                  event.preventDefault();
                  void handleResetPassword();
                }}
                placeholder={text('AUTH_PASSWORD_CONFIRM_PLACEHOLDER', '비밀번호를 다시 입력해 주세요.')}
                disabled={!isResetCodeVerified}
              />
              <p className={`header-auth-field-hint ${newPasswordConfirm.length > 0 && !isResetPasswordConfirmValid ? 'is-error' : newPasswordConfirm.length > 0 ? 'is-success' : ''}`}>
                {text('AUTH_PASSWORD_CONFIRM_VALIDATION_MESSAGE', '비밀번호 확인은 비밀번호와 동일해야 합니다.')}
              </p>
              {resetPasswordChangedStatusMessage ? <p className="header-auth-field-hint is-success">{resetPasswordChangedStatusMessage}</p> : null}
            </div>

            {resetErrors.length > 0 ? (
              <div className="header-auth-feedback is-error" role="alert">
                {resetErrors.map((reason) => (
                  <p key={reason}>{reason}</p>
                ))}
              </div>
            ) : null}

            <div className="header-auth-reset-actions">
              <button
                type="submit"
                className="btn primary"
                disabled={!isResetCodeVerified || !isResetPasswordValid || !isResetPasswordConfirmValid || isResettingPassword}
              >
                {isResettingPassword ? text('AUTH_PASSWORD_CHANGING_LABEL', '변경 중') : text('AUTH_PASSWORD_CHANGE_BUTTON', '비밀번호 변경')}
              </button>
              <button type="button" className="header-auth-reset-link" onClick={() => setMode('login')}>
                {text('AUTH_BACK_TO_LOGIN_BUTTON', '로그인으로 돌아가기')}
              </button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}
