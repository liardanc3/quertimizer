import { useSyncExternalStore, useState } from 'react';
import { DEFAULT_PROBLEM_PATH, LANDING_SIGNUP_PATH, navigate } from '../lib/navigation';
import { loginMock } from '../lib/session';

const SIGNUP_ID_PATTERN = /^[A-Za-z0-9_-]{1,20}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

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
  return value.replace(/[^A-Za-z0-9_-]/g, '').slice(0, 20);
}

function hasRequiredPasswordFormat(value: string) {
  return value.length >= 8 && /[^A-Za-z0-9]/.test(value);
}

const signupGuideLines = [
  'ID는 한 번 설정하면 변경할 수 없습니다.',
  '계정 복구를 위해 사용할 이메일을 정확하게 입력해 주세요.',
];

export default function PublicHomePage() {
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [signupHandle, setSignupHandle] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [signupPasswordConfirm, setSignupPasswordConfirm] = useState('');
  const [email, setEmail] = useState('');
  const hash = useSyncExternalStore(subscribe, getSnapshot, () => '');
  const isSignupOpen = hash === '#signup';
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
    isSignupEmailValid;

  function handleLogin() {
    loginMock();
    navigate(DEFAULT_PROBLEM_PATH);
  }

  function handleSignup() {
    if (!isSignupReady) {
      return;
    }

    loginMock();
    navigate(DEFAULT_PROBLEM_PATH);
  }

  function openSignup() {
    navigate(LANDING_SIGNUP_PATH);
  }

  function closeSignup() {
    navigate('/', { replace: true });
  }

  return (
    <div className={`public-home-shell ${isSignupOpen ? 'is-signup' : ''}`}>
      {isSignupOpen ? (
        <div className="signup-overlay-layout" id="auth-form">
          <div className="signup-close-row">
            <button
              type="button"
              className="signup-close-button"
              onClick={closeSignup}
              aria-label="회원가입 닫기"
            >
              ×
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
                <p className="panel-meta">회원가입</p>
              </div>

              <div className="field-stack">
                <label className="field-label" htmlFor="signup-handle">
                  ID
                </label>
                <input
                  id="signup-handle"
                  className="text-field"
                  value={signupHandle}
                  onChange={(event) => setSignupHandle(sanitizeSignupId(event.target.value))}
                  placeholder="사용할 ID를 입력하세요"
                  autoComplete="username"
                  maxLength={20}
                  aria-invalid={signupHandle.length > 0 && !isSignupIdValid}
                />
                <p className="hint-text signup-field-hint">
                  영문, 숫자, 언더스코어(_)와 하이픈(-)만 사용할 수 있으며 최대 20자까지 입력할 수 있습니다.
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
                  onChange={(event) => setSignupPassword(event.target.value)}
                  placeholder="비밀번호를 입력하세요"
                  autoComplete="new-password"
                  aria-invalid={signupPassword.length > 0 && !isSignupPasswordValid}
                />
                <p className={`hint-text signup-field-hint ${signupPassword.length > 0 && !isSignupPasswordValid ? 'is-error' : ''}`}>
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
                  onChange={(event) => setSignupPasswordConfirm(event.target.value)}
                  placeholder="비밀번호를 다시 입력하세요"
                  autoComplete="new-password"
                  aria-invalid={signupPasswordConfirm.length > 0 && !isSignupPasswordConfirmValid}
                />
                <p
                  className={`hint-text signup-field-hint ${
                    signupPasswordConfirm.length > 0 && !isSignupPasswordConfirmValid ? 'is-error' : ''
                  }`}
                >
                  비밀번호 확인은 위 비밀번호와 동일하게 입력해 주세요.
                </p>
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
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="이메일을 입력하세요"
                  autoComplete="email"
                  inputMode="email"
                  aria-invalid={email.length > 0 && !isSignupEmailValid}
                />
                <p className={`hint-text signup-field-hint ${email.length > 0 && !isSignupEmailValid ? 'is-error' : ''}`}>
                  올바른 이메일 형식으로 입력해 주세요.
                </p>
              </div>

              <button
                type="button"
                className="btn primary full-width"
                onClick={handleSignup}
                disabled={!isSignupReady}
              >
                가입하기
              </button>
            </section>
          </section>
        </div>
      ) : (
        <section className="public-home-content" id="auth-form">
          <img className="mobile-landing-logo" src="/favicon.svg" alt="quertimizer" />

          <h1 className="landing-title-block">
            <span className="landing-title-primary">정답과 성능을 함께 평가하는</span>
            <span className="landing-title-secondary">SQL 문제 풀이 플랫폼</span>
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
                onChange={(event) => setUserId(event.target.value)}
                placeholder="아이디를 입력하세요"
                autoComplete="username"
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
                onChange={(event) => setPassword(event.target.value)}
                placeholder="비밀번호를 입력하세요"
                autoComplete="current-password"
              />
            </div>

            <div className="auth-actions minimal">
              <button type="button" className="btn primary" onClick={handleLogin}>
                로그인
              </button>
              <button type="button" className="btn secondary" onClick={openSignup}>
                회원가입
              </button>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
