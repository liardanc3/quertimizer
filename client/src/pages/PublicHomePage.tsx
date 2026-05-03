import { useEffect, useRef, useSyncExternalStore, useState, type RefObject } from 'react';
import AccountRecoveryOverlay from '../components/home/AccountRecoveryOverlay';
import logoImage from '../assets/logo.png';
import './PublicHomePage.css';
import {
  AuthApiError,
  SignupApiError,
  checkDuplicateEmail,
  fetchSessionMe,
  login,
  sendSignupVerificationCode,
  startGoogleLogin,
  startGithubLogin,
  startKakaoLogin,
  signup,
  verifySignupVerificationCode,
} from '../lib/authApi';
import type { DuplicateCheckResult, SessionMeResult } from '../lib/authApi';
import { completeAuthentication } from '../lib/authSession';
import {
  DEFAULT_PROBLEM_PATH,
  LANDING_RESET_PASSWORD_PATH,
  LANDING_SIGNUP_PATH,
  navigate,
} from '../lib/navigation';
import {
  EMAIL_PATTERN,
  PASSWORD_RESET_CODE_PATTERN,
  getAuthSocialLoginErrorMessage,
  hasRequiredPasswordFormat,
  sanitizeVerificationCode,
} from '../lib/authUi';
import { getUiTextValue, useUiText } from '../lib/uiText';

const DUPLICATED_EMAIL_REASON = getUiTextValue('AUTH_EMAIL_DUPLICATED_MESSAGE', '이미 사용 중인 이메일입니다.');

type DuplicateCheckStatus = 'idle' | 'checking' | 'available' | 'duplicated';

function subscribe(callback: () => void) {
  window.addEventListener('popstate', callback);
  window.addEventListener('hashchange', callback);

  return () => {
    window.removeEventListener('popstate', callback);
    window.removeEventListener('hashchange', callback);
  };
}

function getSnapshot() {
  return window.location.hash;
}

function clearLandingQuery() {
  const nextUrl = `${window.location.pathname}${window.location.hash}`;
  window.history.replaceState(window.history.state ?? {}, '', nextUrl);
}

function consumeSolvePageAuthReturnPath() {
  if (typeof window === 'undefined') {
    return null;
  }

  const storedValue = window.sessionStorage.getItem('quertimizer.solve-auth-return');
  if (!storedValue) {
    return null;
  }

  try {
    const parsedValue = JSON.parse(storedValue) as { path?: string };
    return typeof parsedValue.path === 'string' && parsedValue.path.trim() !== '' ? parsedValue.path : null;
  } catch {
    return null;
  }
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

export default function PublicHomePage() {
  const { text } = useUiText();
  const hash = useSyncExternalStore(subscribe, getSnapshot, () => '');
  const isSignupOpen = hash === '#signup';
  const isResetPasswordOpen = hash === '#reset-password';
  const isOverlayOpen = isSignupOpen || isResetPasswordOpen;
  const signupTitle = text('PUBLIC_HOME_SIGNUP_TITLE', '회원가입');
  const resetTitle = text('AUTH_RESET_TITLE', '비밀번호 찾기');
  const overlayPageTitle = isSignupOpen ? signupTitle : isResetPasswordOpen ? resetTitle : null;

  const [loginEmail, setLoginEmail] = useState('');
  const [password, setPassword] = useState('');
  const [signupCode, setSignupCode] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [signupPasswordConfirm, setSignupPasswordConfirm] = useState('');
  const [email, setEmail] = useState('');
  const [loginErrorReasons, setLoginErrorReasons] = useState<string[]>([]);
  const [isLoginSubmitting, setIsLoginSubmitting] = useState(false);
  const [signupErrorReasons, setSignupErrorReasons] = useState<string[]>([]);
  const [signupStatusMessage, setSignupStatusMessage] = useState<string | null>(null);
  const [isSignupSubmitting, setIsSignupSubmitting] = useState(false);
  const [isSendingSignupCode, setIsSendingSignupCode] = useState(false);
  const [isVerifyingSignupCode, setIsVerifyingSignupCode] = useState(false);
  const [isSignupCodeSent, setIsSignupCodeSent] = useState(false);
  const [isSignupCodeVerified, setIsSignupCodeVerified] = useState(false);
  const [signupEmailCheckStatus, setSignupEmailCheckStatus] = useState<DuplicateCheckStatus>('idle');
  const [signupEmailCheckReason, setSignupEmailCheckReason] = useState<string | null>(null);
  const [signupEmailLastCheckedValue, setSignupEmailLastCheckedValue] = useState('');
  const signupEmailCheckSequenceRef = useRef(0);
  const signupCodeInputRef = useRef<HTMLInputElement | null>(null);
  const signupPasswordInputRef = useRef<HTMLInputElement | null>(null);
  const signupPasswordConfirmInputRef = useRef<HTMLInputElement | null>(null);

  const prefilledLoginEmail =
    typeof window.history.state?.prefillLoginEmail === 'string'
      ? window.history.state.prefillLoginEmail.trim()
      : '';
  const shouldFocusLoginPassword = window.history.state?.focusLoginPassword === true;

  const normalizedLoginEmail = loginEmail.trim();
  const normalizedEmail = email.trim();
  const normalizedSignupCode = signupCode.trim().toUpperCase();

  const isLoginReady = normalizedLoginEmail !== '' && password.trim() !== '';
  const isSignupPasswordValid = hasRequiredPasswordFormat(signupPassword);
  const isSignupPasswordConfirmValid = signupPasswordConfirm !== '' && signupPassword === signupPasswordConfirm;
  const isSignupEmailValid = EMAIL_PATTERN.test(normalizedEmail);
  const isSignupCodeValid = PASSWORD_RESET_CODE_PATTERN.test(normalizedSignupCode);
  const signupEmailHint = text('AUTH_EMAIL_HINT', '올바른 이메일 형식으로 입력해 주세요.');
  const signupEmailCheckingMessage = text('PUBLIC_HOME_SIGNUP_CHECKING_MESSAGE', '이메일 중복을 확인하고 있습니다.');
  const signupEmailAvailableMessage = text('AUTH_EMAIL_AVAILABLE_MESSAGE', '사용 가능한 이메일입니다.');
  const signupCodeHint = text('AUTH_CODE_HINT', '이메일로 받은 인증코드 6자를 입력해 주세요.');
  const signupCodeSentMessage = text('AUTH_CODE_SENT_MESSAGE', '인증 코드를 전송했습니다. 5분 이내에 입력해 주세요.');
  const signupCodeVerifiedMessage = text('AUTH_CODE_VERIFIED_MESSAGE', '인증 코드가 확인되었습니다. 비밀번호를 입력해 주세요.');
  const signupPasswordValidationMessage = text('AUTH_PASSWORD_VALIDATION_MESSAGE', '비밀번호는 특수문자를 포함해 8자 이상이어야 합니다.');
  const signupPasswordConfirmMessage = text('AUTH_PASSWORD_CONFIRM_MATCH_MESSAGE', '비밀번호 확인은 비밀번호와 동일하게 입력해 주세요.');

  const isEmailSignupReady =
    isSignupPasswordValid &&
    isSignupPasswordConfirmValid &&
    isSignupEmailValid &&
    isSignupCodeVerified &&
    signupEmailCheckStatus !== 'checking';

  const signupCodeSentStatusMessage = signupStatusMessage === signupCodeSentMessage ? signupStatusMessage : null;
  const signupCodeVerifiedStatusMessage = signupStatusMessage === signupCodeVerifiedMessage ? signupStatusMessage : null;
  const signupEmailHintMessage =
    signupCodeSentStatusMessage ??
    (normalizedEmail === ''
      ? signupEmailHint
      : !isSignupEmailValid
        ? signupEmailHint
        : signupEmailCheckStatus === 'checking'
          ? signupEmailCheckingMessage
          : signupEmailLastCheckedValue === normalizedEmail && signupEmailCheckStatus === 'duplicated' && signupEmailCheckReason
            ? signupEmailCheckReason
            : signupEmailLastCheckedValue === normalizedEmail && signupEmailCheckStatus === 'available'
              ? signupEmailAvailableMessage
              : signupEmailHint);

  const hasSignupEmailError =
    normalizedEmail !== '' &&
    (!isSignupEmailValid || (signupEmailLastCheckedValue === normalizedEmail && signupEmailCheckStatus === 'duplicated'));
  const hasSignupEmailSuccess =
    normalizedEmail !== '' &&
    !hasSignupEmailError &&
    ((signupEmailLastCheckedValue === normalizedEmail && signupEmailCheckStatus === 'available') || signupCodeSentStatusMessage != null);
  const hasSignupPasswordSuccess = signupPassword.length > 0 && isSignupPasswordValid;
  const hasSignupPasswordConfirmSuccess = signupPasswordConfirm.length > 0 && isSignupPasswordConfirmValid;

  function focusNextInput(nextInputRef: RefObject<HTMLInputElement | null>) {
    window.requestAnimationFrame(() => {
      nextInputRef.current?.focus();
    });
  }

  useEffect(() => {
    if (prefilledLoginEmail === '') {
      return;
    }

    setLoginEmail(prefilledLoginEmail);
    setPassword('');
    setLoginErrorReasons([]);

    const nextState = { ...(window.history.state ?? {}) };
    delete nextState.prefillLoginEmail;
    delete nextState.focusLoginPassword;
    window.history.replaceState(nextState, '', window.location.href);

    if (!shouldFocusLoginPassword) {
      return;
    }

    window.requestAnimationFrame(() => {
      const passwordInput = document.getElementById('user-password');
      if (passwordInput instanceof HTMLInputElement) {
        passwordInput.focus();
      }
    });
  }, [prefilledLoginEmail, shouldFocusLoginPassword]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const socialLoginSuccess = params.get('socialLoginSuccess');
    const socialLoginError = params.get('socialLoginError');

    if (socialLoginSuccess == null && socialLoginError == null) {
      return;
    }

    let isDisposed = false;

    async function handleSocialLoginState() {
      const isPopupWindow = window.opener != null && !window.opener.closed;

      if (socialLoginError != null) {
        if (isPopupWindow) {
          window.opener.postMessage(
            {
              type: 'quertimizer-social-login-error',
              provider: socialLoginError,
            },
            window.location.origin,
          );
          clearLandingQuery();
          window.close();
          return;
        }

        if (!isDisposed) {
          setLoginErrorReasons([getAuthSocialLoginErrorMessage(socialLoginError)]);
        }
        clearLandingQuery();
        return;
      }

      try {
        if (isPopupWindow) {
          window.opener.postMessage(
            {
              type: 'quertimizer-social-login-success',
              provider: socialLoginSuccess,
            },
            window.location.origin,
          );
          clearLandingQuery();
          window.close();
          return;
        }

        const session = await fetchSessionMe();
        if (isDisposed) {
          return;
        }

        await handleAuthenticatedUser(session, { useSolveReturnPath: true });
      } catch {
        if (!isDisposed) {
          setLoginErrorReasons([getAuthSocialLoginErrorMessage(socialLoginSuccess)]);
        }
      } finally {
        clearLandingQuery();
      }
    }

    void handleSocialLoginState();

    return () => {
      isDisposed = true;
    };
  }, []);

  function resetSignupEmailCheck() {
    setSignupEmailCheckStatus('idle');
    setSignupEmailCheckReason(null);
    setSignupEmailLastCheckedValue('');
  }

  function resetSignupVerification() {
    setSignupCode('');
    setSignupStatusMessage(null);
    setIsSignupCodeSent(false);
    setIsSignupCodeVerified(false);
  }

  function applySignupErrorReasons(reasons: string[]) {
    const nextSignupErrorReasons: string[] = [];

    for (const reason of reasons) {
      if (reason === DUPLICATED_EMAIL_REASON) {
        setSignupEmailCheckStatus('duplicated');
        setSignupEmailCheckReason(reason);
        setSignupEmailLastCheckedValue(normalizedEmail);
        continue;
      }

      nextSignupErrorReasons.push(reason);
    }

    setSignupErrorReasons(nextSignupErrorReasons);
  }
  async function checkSignupEmailDuplication() {
    const currentEmail = email.trim();

    if (currentEmail === '') {
      resetSignupEmailCheck();
      return false;
    }

    if (!EMAIL_PATTERN.test(currentEmail)) {
      resetSignupEmailCheck();
      return false;
    }

    if (signupEmailLastCheckedValue === currentEmail) {
      return signupEmailCheckStatus === 'available';
    }

    const requestSequence = signupEmailCheckSequenceRef.current + 1;
    signupEmailCheckSequenceRef.current = requestSequence;
    setSignupEmailCheckStatus('checking');
    setSignupEmailCheckReason(null);
    setSignupErrorReasons([]);

    try {
      const result: DuplicateCheckResult = await checkDuplicateEmail(currentEmail);

      if (requestSequence !== signupEmailCheckSequenceRef.current) {
        return false;
      }

      setSignupEmailCheckStatus(result.available ? 'available' : 'duplicated');
      setSignupEmailCheckReason(result.reason);
      setSignupEmailLastCheckedValue(currentEmail);
      return result.available;
    } catch (error) {
      if (requestSequence !== signupEmailCheckSequenceRef.current) {
        return false;
      }

      setSignupEmailLastCheckedValue(currentEmail);

      if (error instanceof SignupApiError) {
        setSignupEmailCheckStatus('idle');
        setSignupErrorReasons(error.reasons);
        return false;
      }

      setSignupEmailCheckStatus('idle');
      setSignupErrorReasons([error instanceof Error ? error.message : text('AUTH_EMAIL_DUPLICATE_CHECK_FAIL_MESSAGE', '이메일 중복 확인 중 오류가 발생했습니다.')]);
      return false;
    }
  }

  async function handleAuthenticatedUser(session: SessionMeResult, options: { useSolveReturnPath?: boolean } = {}) {
    await completeAuthentication(session);

    if (!session.authenticated) {
      return;
    }

    const solveReturnPath = options.useSolveReturnPath ? consumeSolvePageAuthReturnPath() : null;
    navigate(solveReturnPath ?? DEFAULT_PROBLEM_PATH);
  }

  async function handleLogin() {
    if (!isLoginReady) {
      return;
    }

    try {
      setIsLoginSubmitting(true);
      setLoginErrorReasons([]);

      const session = await login({
        email: normalizedLoginEmail,
        password,
      });

      await handleAuthenticatedUser(session);
    } catch (error) {
      if (error instanceof AuthApiError) {
        setLoginErrorReasons(error.reasons);
        return;
      }

      setLoginErrorReasons([error instanceof Error ? error.message : text('AUTH_LOGIN_ERROR_MESSAGE', '로그인 중 오류가 발생했습니다.')]);
    } finally {
      setIsLoginSubmitting(false);
    }
  }

  async function handleSendSignupCode() {
    if (!isSignupEmailValid || isSendingSignupCode) {
      return false;
    }

    try {
      setIsSendingSignupCode(true);
      setSignupErrorReasons([]);
      setSignupStatusMessage(null);

      const isEmailAvailable = await checkSignupEmailDuplication();
      if (!isEmailAvailable) {
        return false;
      }

      await sendSignupVerificationCode({ email: normalizedEmail });
      setSignupCode('');
      setIsSignupCodeSent(true);
      setIsSignupCodeVerified(false);
      setSignupStatusMessage(signupCodeSentMessage);
      return true;
    } catch (error) {
      if (error instanceof SignupApiError) {
        applySignupErrorReasons(error.reasons);
        return false;
      }

      setSignupErrorReasons([error instanceof Error ? error.message : text('AUTH_CODE_SEND_FAIL_MESSAGE', '인증 코드 전송 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsSendingSignupCode(false);
    }
  }

  async function handleVerifySignupCode() {
    if (!isSignupCodeSent || !isSignupCodeValid || isVerifyingSignupCode) {
      return false;
    }

    try {
      setIsVerifyingSignupCode(true);
      setSignupErrorReasons([]);
      setSignupStatusMessage(null);

      await verifySignupVerificationCode({
        email: normalizedEmail,
        code: normalizedSignupCode,
      });
      setIsSignupCodeVerified(true);
      setSignupStatusMessage(signupCodeVerifiedMessage);
      return true;
    } catch (error) {
      setIsSignupCodeVerified(false);
      if (error instanceof SignupApiError) {
        applySignupErrorReasons(error.reasons);
        return false;
      }

      setSignupErrorReasons([error instanceof Error ? error.message : text('AUTH_CODE_VERIFY_FAIL_MESSAGE', '인증 코드 확인 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsVerifyingSignupCode(false);
    }
  }

  async function handleSignup() {
    if (!isEmailSignupReady) {
      return false;
    }

    try {
      setIsSignupSubmitting(true);
      setSignupErrorReasons([]);

      const isEmailAvailable = await checkSignupEmailDuplication();
      if (!isEmailAvailable) {
        return false;
      }

      await signup({
        password: signupPassword,
        email: normalizedEmail,
        code: normalizedSignupCode,
      });

      const session = await fetchSessionMe();
      await handleAuthenticatedUser(session);
      return true;
    } catch (error) {
      if (error instanceof SignupApiError || error instanceof AuthApiError) {
        applySignupErrorReasons(error.reasons);
        return false;
      }

      setSignupErrorReasons([error instanceof Error ? error.message : text('AUTH_SIGNUP_ERROR_MESSAGE', '회원가입 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsSignupSubmitting(false);
    }
  }

  function openSignup() {
    setLoginErrorReasons([]);
    setSignupErrorReasons([]);
    resetSignupVerification();
    navigate(LANDING_SIGNUP_PATH);
  }

  function openResetPassword() {
    setLoginErrorReasons([]);
    setSignupErrorReasons([]);
    navigate(LANDING_RESET_PASSWORD_PATH);
  }

  function closeOverlay() {
    setLoginErrorReasons([]);
    setSignupErrorReasons([]);
    resetSignupVerification();
    navigate('/', { replace: true });
  }

  function handleGithubLogin() {
    setLoginErrorReasons([]);
    startGithubLogin();
  }

  function handleGoogleLogin() {
    setLoginErrorReasons([]);
    startGoogleLogin();
  }

  function handleKakaoLogin() {
    setLoginErrorReasons([]);
    startKakaoLogin();
  }

  return (
    <div className={`public-home-shell ${isOverlayOpen ? 'is-signup' : ''}`}>
      {isSignupOpen ? (
        <div className="signup-overlay-layout" id="auth-form">
          <div className="signup-close-row" data-title={overlayPageTitle ?? ''}>
            <button
              type="button"
              className="signup-close-button"
              onClick={closeOverlay}
              aria-label={`${overlayPageTitle ?? text('COMMON_STATUS_DIALOG_LABEL', '상태 안내')} ${text('COMMON_CLOSE_BUTTON', '닫기')}`}
            >
              X
            </button>
          </div>

          <section className="signup-split-layout">
            <section className="signup-card">
              <div className="signup-card-header">
                <h1 className="signup-form-title">{signupTitle}</h1>
              </div>

              <>
                  <div className="field-stack">
                    <label className="field-label" htmlFor="signup-email">
                      {text('AUTH_EMAIL_LABEL', '이메일')}
                    </label>
                    <div className="inline-field-row">
                      <input
                        id="signup-email"
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
                        value={email}
                        onChange={(event) => {
                          setEmail(event.target.value);
                          setSignupErrorReasons([]);
                          resetSignupEmailCheck();
                          resetSignupVerification();
                        }}
                        onBlur={() => {
                          void checkSignupEmailDuplication();
                        }}
                        placeholder={text('AUTH_EMAIL_PLACEHOLDER_POLITE', '이메일을 입력해 주세요.')}
                        autoComplete="email"
                        inputMode="email"
                        aria-invalid={hasSignupEmailError}
                      />
                      <button type="button" className="btn secondary fixed-action" onClick={handleSendSignupCode} disabled={!isSignupEmailValid || isSendingSignupCode}>
                        {isSendingSignupCode ? text('COMMON_SENDING_LABEL', '전송 중') : text('AUTH_CODE_SEND_BUTTON', '코드 전송')}
                      </button>
                    </div>
                    <p className={`hint-text signup-field-hint ${hasSignupEmailError ? 'is-error' : hasSignupEmailSuccess ? 'is-success' : ''}`}>
                      {signupEmailHintMessage}
                    </p>
                  </div>

                  <div className="field-stack">
                    <label className="field-label" htmlFor="signup-code">
                      {text('AUTH_CODE_LABEL', '인증 코드')}
                    </label>
                    <div className="inline-field-row">
                      <input
                        id="signup-code"
                        type="text"
                        className="text-field"
                        ref={signupCodeInputRef}
                        value={signupCode}
                        onChange={(event) => {
                          setSignupCode(sanitizeVerificationCode(event.target.value));
                          setSignupErrorReasons([]);
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
                        placeholder={text('PUBLIC_HOME_SIGNUP_CODE_PLACEHOLDER', '인증코드 6자를 입력하세요')}
                        inputMode="text"
                        autoComplete="one-time-code"
                        maxLength={6}
                        disabled={!isSignupCodeSent}
                      />
                      <button
                        type="button"
                        className="btn secondary fixed-action"
                        onClick={handleVerifySignupCode}
                        disabled={!isSignupCodeSent || !isSignupCodeValid || isVerifyingSignupCode}
                      >
                        {isVerifyingSignupCode ? text('COMMON_VERIFYING_LABEL', '확인 중') : text('AUTH_CODE_VERIFY_BUTTON', '코드 확인')}
                      </button>
                    </div>
                    {signupCodeVerifiedStatusMessage ? <p className="hint-text signup-field-hint is-success">{signupCodeVerifiedStatusMessage}</p> : null}
                    {!signupCodeVerifiedStatusMessage ? <p className="hint-text signup-field-hint">{signupCodeHint}</p> : null}
                  </div>

                  <div className="field-stack">
                    <label className="field-label" htmlFor="signup-password">
                      {text('AUTH_PASSWORD_LABEL', '비밀번호')}
                    </label>
                    <input
                      id="signup-password"
                      type="password"
                      className="text-field"
                      ref={signupPasswordInputRef}
                      value={signupPassword}
                      onChange={(event) => {
                        setSignupPassword(event.target.value);
                        setSignupErrorReasons([]);
                      }}
                      onKeyDown={(event) => {
                        if (event.key !== 'Enter') {
                          return;
                        }

                        event.preventDefault();
                        focusNextInput(signupPasswordConfirmInputRef);
                      }}
                      placeholder={text('AUTH_PASSWORD_PLACEHOLDER_POLITE', '비밀번호를 입력해 주세요.')}
                      autoComplete="new-password"
                      aria-invalid={signupPassword.length > 0 && !isSignupPasswordValid}
                    />
                    <p
                      className={`hint-text signup-field-hint ${
                        signupPassword.length > 0 && !isSignupPasswordValid ? 'is-error' : hasSignupPasswordSuccess ? 'is-success' : ''
                      }`}
                    >
                      {signupPasswordValidationMessage}
                    </p>
                  </div>

                  <div className="field-stack">
                    <label className="field-label" htmlFor="signup-password-confirm">
                      {text('AUTH_PASSWORD_CONFIRM_LABEL', '비밀번호 확인')}
                    </label>
                    <input
                      id="signup-password-confirm"
                      type="password"
                      className="text-field"
                      ref={signupPasswordConfirmInputRef}
                      value={signupPasswordConfirm}
                      onChange={(event) => {
                        setSignupPasswordConfirm(event.target.value);
                        setSignupErrorReasons([]);
                      }}
                      onKeyDown={(event) => {
                        if (event.key !== 'Enter') {
                          return;
                        }

                        event.preventDefault();
                        void handleSignup();
                      }}
                      placeholder={text('AUTH_PASSWORD_CONFIRM_PLACEHOLDER', '비밀번호를 다시 입력해 주세요.')}
                      autoComplete="new-password"
                      aria-invalid={signupPasswordConfirm.length > 0 && !isSignupPasswordConfirmValid}
                    />
                    <p
                      className={`hint-text signup-field-hint ${
                        signupPasswordConfirm.length > 0 && !isSignupPasswordConfirmValid
                          ? 'is-error'
                          : hasSignupPasswordConfirmSuccess
                            ? 'is-success'
                            : ''
                      }`}
                    >
                      {signupPasswordConfirmMessage}
                    </p>
                  </div>
              </>

              {signupErrorReasons.length > 0 ? (
                <div className="signup-feedback-box" role="alert" aria-live="polite">
                  {signupErrorReasons.map((reason) => (
                    <p key={reason} className="signup-feedback-message">
                      {reason}
                    </p>
                  ))}
                </div>
              ) : null}

              <button
                type="button"
                className="btn primary full-width"
                onClick={() => void handleSignup()}
                disabled={!isEmailSignupReady || isSignupSubmitting}
              >
                {isSignupSubmitting ? text('PUBLIC_HOME_SIGNUP_PROGRESS_LABEL', '처리 중...') : text('AUTH_SIGNUP_BUTTON', '가입하기')}
              </button>
            </section>
          </section>
        </div>
      ) : isResetPasswordOpen ? (
        <AccountRecoveryOverlay onClose={closeOverlay} />
      ) : (
        <section className="public-home-content" id="auth-form">
          <img className="mobile-landing-logo" src={logoImage} alt="quertimizer" />

          <h1 className="landing-title-block">
            <span className="landing-title-primary">{text('PUBLIC_HOME_TITLE_PRIMARY', '정답과 성능을 함께 평가하는')}</span>
            <span className="landing-title-secondary">{text('PUBLIC_HOME_TITLE_SECONDARY', 'SQL 문제 학습 플랫폼')}</span>
          </h1>

          <div className="minimal-auth-form">
            <div className="landing-auth-layout">
              <section className="landing-login-panel" aria-label={text('AUTH_LOGIN_FORM_LABEL', '로그인 입력')}>
                <div className="field-stack">
                  <label className="field-label" htmlFor="login-email">
                    {text('AUTH_EMAIL_LABEL', '이메일')}
                  </label>
                  <input
                    id="login-email"
                    type="email"
                    className="text-field"
                    value={loginEmail}
                    onChange={(event) => {
                      setLoginEmail(event.target.value);
                      setLoginErrorReasons([]);
                    }}
                    placeholder={text('AUTH_LOGIN_EMAIL_PLACEHOLDER', '이메일을 입력하세요')}
                    autoComplete="email"
                    inputMode="email"
                  />
                </div>

                <div className="field-stack">
                  <label className="field-label" htmlFor="user-password">
                    {text('AUTH_PASSWORD_LABEL', '비밀번호')}
                  </label>
                  <input
                    id="user-password"
                    type="password"
                    className="text-field"
                    value={password}
                    onChange={(event) => {
                      setPassword(event.target.value);
                      setLoginErrorReasons([]);
                    }}
                    onKeyDown={(event) => {
                      if (event.key !== 'Enter') {
                        return;
                      }

                      event.preventDefault();
                      void handleLogin();
                    }}
                    placeholder={text('AUTH_LOGIN_PASSWORD_PLACEHOLDER', '비밀번호를 입력하세요')}
                    autoComplete="current-password"
                  />
                </div>

                {loginErrorReasons.length > 0 ? (
                  <div className="signup-feedback-box" role="alert" aria-live="polite">
                    {loginErrorReasons.map((reason) => (
                      <p key={reason} className="signup-feedback-message">
                        {reason}
                      </p>
                    ))}
                  </div>
                ) : null}

                <div className="auth-actions minimal">
                  <button
                    type="button"
                    className="btn primary landing-login-submit"
                    onClick={() => void handleLogin()}
                    disabled={!isLoginReady || isLoginSubmitting}
                  >
                    {isLoginSubmitting ? text('AUTH_LOGIN_IN_PROGRESS_ELLIPSIS', '로그인 중...') : text('AUTH_LOGIN_TITLE', '로그인')}
                  </button>
                </div>

                <button type="button" className="btn text landing-password-reset-link" onClick={openResetPassword}>
                  {text('AUTH_FORGOT_PASSWORD_LINK', '비밀번호를 잊으셨나요?')}
                </button>
              </section>

              <div className="landing-auth-divider" aria-hidden="true">
                <span className="landing-auth-divider-line" />
                <img className="landing-auth-divider-mark" src={logoImage} alt="" />
                <span className="landing-auth-divider-line" />
              </div>

              <aside className="landing-access-panel" aria-label={text('AUTH_ACCOUNT_SUPPORT_LABEL', '계정 지원')}>
                <div className="landing-access-group landing-access-group-social">
                  <button type="button" className="landing-access-card is-social" onClick={handleGoogleLogin}>
                    <span className="landing-access-card-icon" aria-hidden="true">
                      <GoogleMarkIcon />
                    </span>
                    <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_GOOGLE', 'Google로 계속하기')}</span>
                  </button>

                  <button type="button" className="landing-access-card is-social" onClick={handleGithubLogin}>
                    <span className="landing-access-card-icon" aria-hidden="true">
                      <GithubMarkIcon />
                    </span>
                    <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_GITHUB', 'Github로 계속하기')}</span>
                  </button>

                  <button type="button" className="landing-access-card is-social" onClick={handleKakaoLogin}>
                    <span className="landing-access-card-icon" aria-hidden="true">
                      <KakaoMarkIcon />
                    </span>
                    <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_KAKAO', 'Kakao로 계속하기')}</span>
                  </button>
                </div>

                <div className="landing-access-group landing-access-group-support">
                  <button type="button" className="landing-access-card is-social is-email" onClick={openSignup}>
                    <span className="landing-access-card-icon" aria-hidden="true">
                      <EmailMarkIcon />
                    </span>
                    <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_EMAIL', '이메일로 계속하기')}</span>
                  </button>
                </div>
              </aside>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
