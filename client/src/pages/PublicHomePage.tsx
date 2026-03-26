import { useSyncExternalStore, useState } from 'react';
import { DEFAULT_PROBLEM_PATH, LANDING_SIGNUP_PATH, navigate } from '../lib/navigation';
import { loginMock } from '../lib/session';

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

const signupGuideLines = [
  '핸들(ID)는 한번 설정하면 변경할 수 없습니다.',
  '랭킹, 제출 기록, 공개 프로필에 함께 노출될 식별자이니 신중하게 정해주세요.',
];

export default function PublicHomePage() {
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [signupHandle, setSignupHandle] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [email, setEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [isCodeVerified, setIsCodeVerified] = useState(false);
  const hash = useSyncExternalStore(subscribe, getSnapshot, () => '');
  const isSignupOpen = hash === '#signup';

  function handleLogin() {
    loginMock();
    navigate(DEFAULT_PROBLEM_PATH);
  }

  function handleSignup() {
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
            <button type="button" className="signup-close-button" onClick={closeSignup} aria-label="회원가입 닫기">
              ×
            </button>
          </div>

          <section className="signup-split-layout">
            <div className="signup-guide-panel">
              <p className="panel-meta">Signup Guide</p>
              <h1 className="page-title signup-guide-heading">회원가입 안내</h1>
              <div className="signup-guide-copy">
                {signupGuideLines.map((line, index) => (
                  <p key={line} className={`signup-guide-message ${index === 1 ? 'is-compact' : ''}`}>
                    {line}
                  </p>
                ))}
              </div>
            </div>

            <section className="signup-card">
              <div className="signup-card-header">
                <p className="panel-meta">Sign Up</p>
                <h2 className="panel-title">회원가입</h2>
              </div>

              <div className="field-stack">
                <label className="field-label" htmlFor="signup-handle">
                  핸들(ID)
                </label>
                <input
                  id="signup-handle"
                  className="text-field"
                  value={signupHandle}
                  onChange={(event) => setSignupHandle(event.target.value)}
                  placeholder="사용할 핸들을 입력하세요"
                />
              </div>

              <div className="field-stack">
                <label className="field-label" htmlFor="signup-password">
                  PW
                </label>
                <input
                  id="signup-password"
                  type="password"
                  className="text-field"
                  value={signupPassword}
                  onChange={(event) => setSignupPassword(event.target.value)}
                  placeholder="비밀번호를 입력하세요"
                />
              </div>

              <div className="field-stack">
                <label className="field-label" htmlFor="signup-email">
                  이메일
                </label>
                <div className="inline-field-row">
                  <input
                    id="signup-email"
                    className="text-field"
                    value={email}
                    onChange={(event) => {
                      setEmail(event.target.value);
                      setIsEmailVerified(false);
                      setIsCodeVerified(false);
                    }}
                    placeholder="이메일을 입력하세요"
                    autoComplete="email"
                  />
                  {isEmailVerified ? (
                    <span className="verification-state" aria-label="이메일 전송 완료">
                      전송완료
                    </span>
                  ) : (
                    <button
                      type="button"
                      className="btn secondary fixed-action"
                      onClick={() => setIsEmailVerified(true)}
                    >
                      인증하기
                    </button>
                  )}
                </div>
              </div>

              <div className="field-stack">
                <label className="field-label" htmlFor="verification-code">
                  인증번호
                </label>
                <div className="inline-field-row">
                  <input
                    id="verification-code"
                    className="text-field"
                    value={verificationCode}
                    onChange={(event) => {
                      setVerificationCode(event.target.value);
                      setIsCodeVerified(false);
                    }}
                    placeholder="인증번호를 입력하세요"
                  />
                  {isCodeVerified ? (
                    <span className="verification-check" aria-label="인증번호 확인 완료">
                      ✓
                    </span>
                  ) : (
                    <button
                      type="button"
                      className="btn secondary fixed-action"
                      onClick={() => setIsCodeVerified(true)}
                      disabled={!isEmailVerified}
                    >
                      확인
                    </button>
                  )}
                </div>
              </div>

              <button
                type="button"
                className="btn primary full-width"
                onClick={handleSignup}
                disabled={!isCodeVerified}
              >
                가입하기
              </button>
            </section>
          </section>
        </div>
      ) : (
        <section className="public-home-content" id="auth-form">
          <img className="mobile-landing-logo" src="/favicon.svg" alt="speedql" />

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
                PW
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
