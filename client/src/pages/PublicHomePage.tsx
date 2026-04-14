import { useEffect, useRef, useSyncExternalStore, useState } from 'react';
import AccountRecoveryOverlay from '../components/home/AccountRecoveryOverlay';
import logoImage from '../assets/logo.svg';
import './PublicHomePage.css';
import {
  AuthApiError,
  SignupApiError,
  checkDuplicateEmail,
  checkDuplicateUserId,
  login,
  signup,
} from '../lib/authApi';
import type { DuplicateCheckResult } from '../lib/authApi';
import { completeAuthentication } from '../lib/authSession';
import {
  DEFAULT_PROBLEM_PATH,
  LANDING_FIND_USER_ID_PATH,
  LANDING_RESET_PASSWORD_PATH,
  LANDING_SIGNUP_PATH,
  navigate,
} from '../lib/navigation';
import { useHomeSiteTitle } from '../lib/uiText';

const SIGNUP_ID_PATTERN = /^[A-Za-z0-9_-]{1,15}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SIGNUP_ID_HINT = '영문, 숫자, 언더스코어(_)와 하이픈(-)만 사용할 수 있으며 최대 15자까지 입력할 수 있습니다.';
const SIGNUP_EMAIL_HINT = '올바른 이메일 형식으로 입력해 주세요.';
const SIGNUP_ID_CHECKING_MESSAGE = '아이디 중복을 확인하고 있습니다.';
const SIGNUP_EMAIL_CHECKING_MESSAGE = '이메일 중복을 확인하고 있습니다.';
const SIGNUP_ID_AVAILABLE_MESSAGE = '사용 가능한 아이디입니다.';
const SIGNUP_EMAIL_AVAILABLE_MESSAGE = '사용 가능한 이메일입니다.';
const DUPLICATED_USER_ID_REASON = '이미 사용중인 아이디입니다.';
const DUPLICATED_EMAIL_REASON = '이미 사용중인 이메일입니다.';

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

function sanitizeSignupId(value: string) {
  return value.replace(/[^A-Za-z0-9_-]/g, '').slice(0, 15);
}

function hasRequiredPasswordFormat(value: string) {
  return value.length >= 8 && /[^A-Za-z0-9]/.test(value);
}

const signupGuideLines = [
  'ID는 한번 설정하면 변경할 수 없습니다.',
  '계정 복구를 위해 사용하는 이메일을 정확하게 입력해 주세요.',
];

export default function PublicHomePage() {
  const hash = useSyncExternalStore(subscribe, getSnapshot, () => '');
  const overlayPageTitle =
    hash === '#signup' ? '회원가입' : hash === '#find-user-id' ? '아이디 찾기' : hash === '#reset-password' ? '비밀번호 찾기' : null;

  useHomeSiteTitle(overlayPageTitle);
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [rememberLogin, setRememberLogin] = useState(false);
  const [signupHandle, setSignupHandle] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [signupPasswordConfirm, setSignupPasswordConfirm] = useState('');
  const [email, setEmail] = useState('');
  const [loginErrorReasons, setLoginErrorReasons] = useState<string[]>([]);
  const [isLoginSubmitting, setIsLoginSubmitting] = useState(false);
  const [signupErrorReasons, setSignupErrorReasons] = useState<string[]>([]);
  const [isSignupSubmitting, setIsSignupSubmitting] = useState(false);
  const [signupIdCheckStatus, setSignupIdCheckStatus] = useState<DuplicateCheckStatus>('idle');
  const [signupIdCheckReason, setSignupIdCheckReason] = useState<string | null>(null);
  const [signupIdLastCheckedValue, setSignupIdLastCheckedValue] = useState('');
  const [signupEmailCheckStatus, setSignupEmailCheckStatus] = useState<DuplicateCheckStatus>('idle');
  const [signupEmailCheckReason, setSignupEmailCheckReason] = useState<string | null>(null);
  const [signupEmailLastCheckedValue, setSignupEmailLastCheckedValue] = useState('');
  const signupIdCheckSequenceRef = useRef(0);
  const signupEmailCheckSequenceRef = useRef(0);
  const prefilledLoginId =
    typeof window.history.state?.prefillLoginId === 'string'
      ? window.history.state.prefillLoginId.slice(0, 15)
      : '';
  const shouldFocusLoginPassword = window.history.state?.focusLoginPassword === true;
  const isSignupOpen = hash === '#signup';
  const isFindUserIdOpen = hash === '#find-user-id';
  const isResetPasswordOpen = hash === '#reset-password';
  const isOverlayOpen = isSignupOpen || isFindUserIdOpen || isResetPasswordOpen;
  const normalizedLoginId = userId.trim();
  const isLoginReady = normalizedLoginId !== '' && password.trim() !== '';
  const normalizedSignupId = signupHandle.trim();
  const normalizedEmail = email.trim();
  const isSignupIdValid = SIGNUP_ID_PATTERN.test(normalizedSignupId);
  const isSignupPasswordValid = hasRequiredPasswordFormat(signupPassword);
  const isSignupPasswordConfirmValid = signupPasswordConfirm !== '' && signupPassword === signupPasswordConfirm;
  const isSignupEmailValid = EMAIL_PATTERN.test(normalizedEmail);
  const isSignupReady =
    isSignupIdValid &&
    isSignupPasswordValid &&
    isSignupPasswordConfirmValid &&
    isSignupEmailValid &&
    signupIdCheckStatus !== 'checking' &&
    signupEmailCheckStatus !== 'checking';
  const signupIdHintMessage =
    normalizedSignupId === ''
      ? SIGNUP_ID_HINT
      : !isSignupIdValid
        ? SIGNUP_ID_HINT
      : signupIdCheckStatus === 'checking'
          ? SIGNUP_ID_CHECKING_MESSAGE
          : signupIdLastCheckedValue === normalizedSignupId && signupIdCheckStatus === 'duplicated' && signupIdCheckReason
            ? signupIdCheckReason
          : signupIdLastCheckedValue === normalizedSignupId && signupIdCheckStatus === 'available'
              ? SIGNUP_ID_AVAILABLE_MESSAGE
              : '';
  const signupEmailHintMessage =
    normalizedEmail === ''
      ? SIGNUP_EMAIL_HINT
      : !isSignupEmailValid
        ? SIGNUP_EMAIL_HINT
        : signupEmailCheckStatus === 'checking'
          ? SIGNUP_EMAIL_CHECKING_MESSAGE
          : signupEmailLastCheckedValue === normalizedEmail && signupEmailCheckStatus === 'duplicated' && signupEmailCheckReason
            ? signupEmailCheckReason
            : signupEmailLastCheckedValue === normalizedEmail && signupEmailCheckStatus === 'available'
              ? SIGNUP_EMAIL_AVAILABLE_MESSAGE
              : SIGNUP_EMAIL_HINT;
  const hasSignupIdError =
    normalizedSignupId !== '' &&
    (!isSignupIdValid || (signupIdLastCheckedValue === normalizedSignupId && signupIdCheckStatus === 'duplicated'));
  const hasSignupEmailError =
    normalizedEmail !== '' &&
    (!isSignupEmailValid || (signupEmailLastCheckedValue === normalizedEmail && signupEmailCheckStatus === 'duplicated'));
  const hasSignupIdSuccess =
    normalizedSignupId !== '' &&
    !hasSignupIdError &&
    signupIdLastCheckedValue === normalizedSignupId &&
    signupIdCheckStatus === 'available';
  const hasSignupEmailSuccess =
    normalizedEmail !== '' &&
    !hasSignupEmailError &&
    signupEmailLastCheckedValue === normalizedEmail &&
    signupEmailCheckStatus === 'available';
  const hasSignupPasswordSuccess = signupPassword.length > 0 && isSignupPasswordValid;
  const hasSignupPasswordConfirmSuccess = signupPasswordConfirm.length > 0 && isSignupPasswordConfirmValid;

  useEffect(() => {
    if (prefilledLoginId === '') {
      return;
    }

    setUserId(prefilledLoginId);
    setPassword('');
    setLoginErrorReasons([]);

    const nextState = { ...(window.history.state ?? {}) };
    delete nextState.prefillLoginId;
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
  }, [prefilledLoginId, shouldFocusLoginPassword]);

  useEffect(() => {
    if (prefilledLoginId === '') {
      return;
    }

    setUserId(prefilledLoginId);
    setPassword('');
    setLoginErrorReasons([]);

    const nextState = { ...(window.history.state ?? {}) };
    delete nextState.prefillLoginId;
    window.history.replaceState(nextState, '', window.location.href);
  }, [prefilledLoginId]);

  function resetSignupUserIdCheck() {
    setSignupIdCheckStatus('idle');
    setSignupIdCheckReason(null);
    setSignupIdLastCheckedValue('');
  }

  function resetSignupEmailCheck() {
    setSignupEmailCheckStatus('idle');
    setSignupEmailCheckReason(null);
    setSignupEmailLastCheckedValue('');
  }

  function applySignupErrorReasons(reasons: string[]) {
    const nextSignupErrorReasons: string[] = [];

    for (const reason of reasons) {
      if (reason === DUPLICATED_USER_ID_REASON) {
        setSignupIdCheckStatus('duplicated');
        setSignupIdCheckReason(reason);
        setSignupIdLastCheckedValue(normalizedSignupId);
        continue;
      }

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

  async function checkSignupUserIdDuplication() {
    const currentSignupId = signupHandle.trim();

    if (currentSignupId === '') {
      resetSignupUserIdCheck();
      return false;
    }

    if (!SIGNUP_ID_PATTERN.test(currentSignupId)) {
      resetSignupUserIdCheck();
      return false;
    }

    if (signupIdLastCheckedValue === currentSignupId) {
      return signupIdCheckStatus === 'available';
    }

    const requestSequence = signupIdCheckSequenceRef.current + 1;
    signupIdCheckSequenceRef.current = requestSequence;
    setSignupIdCheckStatus('checking');
    setSignupIdCheckReason(null);
    setSignupErrorReasons([]);

    try {
      const result: DuplicateCheckResult = await checkDuplicateUserId(currentSignupId);

      if (requestSequence !== signupIdCheckSequenceRef.current) {
        return false;
      }

      setSignupIdCheckStatus(result.available ? 'available' : 'duplicated');
      setSignupIdCheckReason(result.reason);
      setSignupIdLastCheckedValue(currentSignupId);
      return result.available;
    } catch (error) {
      if (requestSequence !== signupIdCheckSequenceRef.current) {
        return false;
      }

      setSignupIdLastCheckedValue(currentSignupId);

      if (error instanceof SignupApiError) {
        setSignupIdCheckStatus('idle');
        setSignupErrorReasons(error.reasons);
        return false;
      }

      setSignupIdCheckStatus('idle');
      setSignupErrorReasons([error instanceof Error ? error.message : '아이디 중복확인 중 알 수 없는 오류가 발생했습니다.']);
      return false;
    }
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
      setSignupErrorReasons([error instanceof Error ? error.message : '이메일 중복확인 중 알 수 없는 오류가 발생했습니다.']);
      return false;
    }
  }

  async function handleAuthenticatedUser(shouldRememberLogin = false) {
    await completeAuthentication(shouldRememberLogin);
    navigate(DEFAULT_PROBLEM_PATH);
  }

  async function handleLogin() {
    if (!isLoginReady) {
      return;
    }

    try {
      setIsLoginSubmitting(true);
      setLoginErrorReasons([]);

      await login({
        userId: normalizedLoginId,
        password,
        rememberLogin,
      });

      await handleAuthenticatedUser(rememberLogin);
    } catch (error) {
      if (error instanceof AuthApiError) {
        setLoginErrorReasons(error.reasons);
        return;
      }

      setLoginErrorReasons([error instanceof Error ? error.message : '로그인 중 알 수 없는 오류가 발생했습니다.']);
    } finally {
      setIsLoginSubmitting(false);
    }
  }

  async function handleSignup() {
    if (!isSignupReady) {
      return;
    }

    try {
      setIsSignupSubmitting(true);
      setSignupErrorReasons([]);

      const isUserIdAvailable = await checkSignupUserIdDuplication();
      const isEmailAvailable = await checkSignupEmailDuplication();

      if (!isUserIdAvailable || !isEmailAvailable) {
        return;
      }

      await signup({
        userId: normalizedSignupId,
        password: signupPassword,
        email: normalizedEmail,
      });

      await handleAuthenticatedUser();
    } catch (error) {
      if (error instanceof SignupApiError || error instanceof AuthApiError) {
        applySignupErrorReasons(error.reasons);
        return;
      }

      setSignupErrorReasons([error instanceof Error ? error.message : '회원가입 중 알 수 없는 오류가 발생했습니다.']);
    } finally {
      setIsSignupSubmitting(false);
    }
  }

  function openSignup() {
    setLoginErrorReasons([]);
    setSignupErrorReasons([]);
    navigate(LANDING_SIGNUP_PATH);
  }

  function openFindUserId() {
    setLoginErrorReasons([]);
    setSignupErrorReasons([]);
    navigate(LANDING_FIND_USER_ID_PATH);
  }

  function openResetPassword() {
    setLoginErrorReasons([]);
    setSignupErrorReasons([]);
    navigate(LANDING_RESET_PASSWORD_PATH);
  }

  function closeOverlay() {
    setLoginErrorReasons([]);
    setSignupErrorReasons([]);
    navigate('/', { replace: true });
  }

  return (
    <div className={`public-home-shell ${isOverlayOpen ? 'is-signup' : ''}`}>
      {isSignupOpen ? (
        <div className="signup-overlay-layout" id="auth-form">
          <div className="signup-close-row" data-title="회원가입">
            <button
              type="button"
              className="signup-close-button"
              onClick={closeOverlay}
              aria-label="회원가입 닫기"
            >
              X
            </button>
          </div>

          <section className="signup-split-layout">
            <div className="signup-guide-panel">
              <p className="panel-meta">회원가입 안내</p>
              <div className="signup-guide-copy">
                {signupGuideLines.map((line, index) => (
                  <p key={line} className={`signup-guide-message ${index > 0 ? 'is-compact' : ''}`}>
                    {line}
                  </p>
                ))}
              </div>
            </div>

            <section className="signup-card">
              <div className="signup-card-header">
                <h1 className="signup-form-title">회원가입</h1>
                <p className="panel-meta">회원가입</p>
              </div>

              <div className="field-stack">
                <label className="field-label" htmlFor="signup-handle">
                  ID
                </label>
                <div className="signup-inline-field signup-inline-field-id">
                <input
                  id="signup-handle"
                  className="text-field"
                  value={signupHandle}
                  onChange={(event) => {
                    setSignupHandle(sanitizeSignupId(event.target.value));
                    setSignupErrorReasons([]);
                    resetSignupUserIdCheck();
                  }}
                  onBlur={() => {
                    void checkSignupUserIdDuplication();
                  }}
                  placeholder="사용할 ID를 입력하세요"
                  autoComplete="username"
                  maxLength={15}
                  aria-invalid={hasSignupIdError}
                />
                </div>
                {signupIdHintMessage ? (
                  <p className={`hint-text signup-field-hint ${hasSignupIdError ? 'is-error' : hasSignupIdSuccess ? 'is-success' : ''}`}>
                    {signupIdHintMessage}
                  </p>
                ) : null}
              </div>

              <div className="field-stack">
                <label className="field-label" htmlFor="signup-email">
                  이메일
                </label>
                <input
                  id="signup-email"
                  type="email"
                  className="text-field"
                  value={email}
                  onChange={(event) => {
                    setEmail(event.target.value);
                    setSignupErrorReasons([]);
                    resetSignupEmailCheck();
                  }}
                  onBlur={() => {
                    void checkSignupEmailDuplication();
                  }}
                  placeholder="이메일을 입력하세요"
                  autoComplete="email"
                  inputMode="email"
                  aria-invalid={hasSignupEmailError}
                />
                <p className={`hint-text signup-field-hint ${hasSignupEmailError ? 'is-error' : hasSignupEmailSuccess ? 'is-success' : ''}`}>
                  {signupEmailHintMessage}
                </p>
              </div>

              <div className="field-stack">
                <label className="field-label" htmlFor="signup-password">
                  비밀번호
                </label>
                <input
                  id="signup-password"
                  type="password"
                  className="text-field"
                  value={signupPassword}
                  onChange={(event) => {
                    setSignupPassword(event.target.value);
                    setSignupErrorReasons([]);
                  }}
                  placeholder="비밀번호를 입력하세요"
                  autoComplete="new-password"
                  aria-invalid={signupPassword.length > 0 && !isSignupPasswordValid}
                />
                <p
                  className={`hint-text signup-field-hint ${
                    signupPassword.length > 0 && !isSignupPasswordValid ? 'is-error' : hasSignupPasswordSuccess ? 'is-success' : ''
                  }`}
                >
                  비밀번호는 특수문자를 포함해 8자 이상이어야 합니다.
                </p>
              </div>

              <div className="field-stack">
                <label className="field-label" htmlFor="signup-password-confirm">
                  비밀번호 확인
                </label>
                <input
                  id="signup-password-confirm"
                  type="password"
                  className="text-field"
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
                  placeholder="비밀번호를 다시 입력하세요"
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
                  비밀번호 확인은 비밀번호와 동일하게 입력해 주세요.
                </p>
              </div>

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
                onClick={handleSignup}
                disabled={!isSignupReady || isSignupSubmitting}
              >
                {isSignupSubmitting ? '가입 중...' : '가입하기'}
              </button>
            </section>
          </section>
        </div>
      ) : isFindUserIdOpen ? (
        <AccountRecoveryOverlay mode="find-user-id" onClose={closeOverlay} />
      ) : isResetPasswordOpen ? (
        <AccountRecoveryOverlay mode="reset-password" onClose={closeOverlay} />
      ) : (
        <section className="public-home-content" id="auth-form">
          <img className="mobile-landing-logo" src={logoImage} alt="quertimizer" />

          <h1 className="landing-title-block">
            <span className="landing-title-primary">정답과 성능을 함께 평가하는</span>
            <span className="landing-title-secondary">SQL 문제 학습 플랫폼</span>
          </h1>

          <div className="minimal-auth-form">
            <div className="field-stack">
              <label className="field-label" htmlFor="user-id">
                아이디
              </label>
              <input
                id="user-id"
                className="text-field"
                value={userId}
                onChange={(event) => {
                  setUserId(event.target.value.slice(0, 15));
                  setLoginErrorReasons([]);
                }}
                placeholder="아이디를 입력하세요"
                autoComplete="username"
                maxLength={15}
              />
            </div>

            <div className="field-stack">
              <label className="field-label" htmlFor="user-password">
                비밀번호
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
                placeholder="비밀번호를 입력하세요"
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

            <label className="login-remember-row">
              <input
                type="checkbox"
                className="login-remember-checkbox"
                checked={rememberLogin}
                onChange={(event) => setRememberLogin(event.target.checked)}
              />
              <span className="login-remember-label">로그인 유지</span>
            </label>

            <div className="auth-actions minimal">
              <button
                type="button"
                className="btn primary"
                onClick={handleLogin}
                disabled={!isLoginReady || isLoginSubmitting}
              >
                {isLoginSubmitting ? '로그인 중...' : '로그인'}
              </button>
              <button type="button" className="btn secondary" onClick={openSignup}>
                회원가입
              </button>
            </div>

            <div className="auth-link-row">
              <button type="button" className="btn text auth-link-button" onClick={openFindUserId}>
                아이디 찾기
              </button>
              <button type="button" className="btn text auth-link-button" onClick={openResetPassword}>
                비밀번호 찾기
              </button>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
