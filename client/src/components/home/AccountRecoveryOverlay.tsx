import { useState } from 'react';
import StatusPopup from '../common/StatusPopup';
import { navigate } from '../../lib/navigation';
import {
  RecoveryApiError,
  findUserId,
  resetPassword,
  sendPasswordResetCode,
  sendUserIdRecoveryCode,
  verifyPasswordResetCode,
} from '../../lib/authApi';

type AccountRecoveryMode = 'find-user-id' | 'reset-password';
type PopupLevel = 1 | 2 | 3;

interface AccountRecoveryOverlayProps {
  mode: AccountRecoveryMode;
  onClose: () => void;
}

interface PopupState {
  level: PopupLevel;
  message: string;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const VERIFICATION_CODE_PATTERN = /^[A-Z0-9]{6}$/;
const VERIFICATION_CODE_HINT = '이메일로 받은 인증코드 6자를 5분 내에 입력해 주세요.';
const PASSWORD_HINT = '비밀번호는 특수문자를 포함해 8자 이상이어야 합니다.';
const PASSWORD_CONFIRM_HINT = '비밀번호 확인은 비밀번호와 동일하게 입력해 주세요.';
const CODE_SENT_MESSAGE = '인증코드를 전송했습니다. 5분 내에 입력해 주세요.';

const findUserIdGuideLines = [
  '가입할 때 사용한 이메일을 입력하면 인증코드를 보내드립니다.',
  '이메일로 받은 인증코드를 5분 내에 입력하면 가입된 아이디를 확인할 수 있습니다.',
];

const resetPasswordGuideLines = [
  '가입할 때 사용한 이메일을 입력하면 인증코드를 보내드립니다.',
  '인증코드 확인이 완료되면 새로운 비밀번호를 바로 설정할 수 있습니다.',
];

function sanitizeVerificationCode(value: string) {
  return value.replace(/[^A-Za-z0-9]/g, '').toUpperCase().slice(0, 6);
}

function hasRequiredPasswordFormat(value: string) {
  return value.length >= 8 && /[^A-Za-z0-9]/.test(value);
}

export default function AccountRecoveryOverlay({ mode, onClose }: AccountRecoveryOverlayProps) {
  const [email, setEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [errorReasons, setErrorReasons] = useState<string[]>([]);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [recoveredUserId, setRecoveredUserId] = useState<string | null>(null);
  const [isCodeSent, setIsCodeSent] = useState(false);
  const [isResetPasswordVerified, setIsResetPasswordVerified] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isVerifyingCode, setIsVerifyingCode] = useState(false);
  const [isResettingPassword, setIsResettingPassword] = useState(false);
  const [popupState, setPopupState] = useState<PopupState | null>(null);
  const isFindUserIdMode = mode === 'find-user-id';
  const normalizedEmail = email.trim();
  const normalizedVerificationCode = verificationCode.trim().toUpperCase();
  const isEmailValid = EMAIL_PATTERN.test(normalizedEmail);
  const isVerificationCodeValid = VERIFICATION_CODE_PATTERN.test(normalizedVerificationCode);
  const isNewPasswordValid = hasRequiredPasswordFormat(newPassword);
  const isNewPasswordConfirmValid = newPasswordConfirm !== '' && newPassword === newPasswordConfirm;
  const canSendCode = isEmailValid && !isSendingCode;
  const canVerifyCode = isCodeSent && isEmailValid && isVerificationCodeValid && !isVerifyingCode;
  const canResetPassword =
    isResetPasswordVerified &&
    isEmailValid &&
    isVerificationCodeValid &&
    isNewPasswordValid &&
    isNewPasswordConfirmValid &&
    !isResettingPassword;
  const overlayTitle = isFindUserIdMode ? '아이디 찾기' : '비밀번호 찾기';
  const guideLines = isFindUserIdMode ? findUserIdGuideLines : resetPasswordGuideLines;
  const emailHintMessage =
    normalizedEmail === ''
      ? '가입할 때 사용한 이메일을 입력해 주세요.'
      : !isEmailValid
        ? '올바른 이메일 형식으로 입력해 주세요.'
        : '인증코드를 받을 수 있는 이메일입니다.';
  const verificationCodeHintMessage = statusMessage ?? VERIFICATION_CODE_HINT;

  function resetRecoveryState() {
    setVerificationCode('');
    setNewPassword('');
    setNewPasswordConfirm('');
    setErrorReasons([]);
    setStatusMessage(null);
    setRecoveredUserId(null);
    setIsCodeSent(false);
    setIsResetPasswordVerified(false);
    setPopupState(null);
  }

  async function handleSendCode() {
    if (!canSendCode) {
      return;
    }

    try {
      setIsSendingCode(true);
      setErrorReasons([]);
      setStatusMessage(null);
      setRecoveredUserId(null);
      setIsResetPasswordVerified(false);
      setPopupState(null);

      if (isFindUserIdMode) {
        await sendUserIdRecoveryCode({ email: normalizedEmail });
      } else {
        await sendPasswordResetCode({ email: normalizedEmail });
      }

      setIsCodeSent(true);
      setVerificationCode('');
      setStatusMessage(CODE_SENT_MESSAGE);
    } catch (error) {
      setErrorReasons(error instanceof RecoveryApiError ? error.reasons : ['인증코드 발송 중 알 수 없는 오류가 발생했습니다.']);
    } finally {
      setIsSendingCode(false);
    }
  }

  async function handleVerifyCode() {
    if (!canVerifyCode) {
      return;
    }

    try {
      setIsVerifyingCode(true);
      setErrorReasons([]);
      setStatusMessage(null);

      if (isFindUserIdMode) {
        const result = await findUserId({
          email: normalizedEmail,
          code: normalizedVerificationCode,
        });

        setRecoveredUserId(result.userId);
        setStatusMessage('인증이 완료되었습니다.');
        return;
      }

      await verifyPasswordResetCode({
        email: normalizedEmail,
        code: normalizedVerificationCode,
      });
      setIsResetPasswordVerified(true);
      setStatusMessage('인증코드가 확인되었습니다. 새 비밀번호를 입력해 주세요.');
    } catch (error) {
      setRecoveredUserId(null);
      setIsResetPasswordVerified(false);
      setErrorReasons(error instanceof RecoveryApiError ? error.reasons : ['인증코드 확인 중 알 수 없는 오류가 발생했습니다.']);
    } finally {
      setIsVerifyingCode(false);
    }
  }

  async function handleResetPassword() {
    if (!canResetPassword) {
      return;
    }

    try {
      setIsResettingPassword(true);
      setErrorReasons([]);

      await resetPassword({
        email: normalizedEmail,
        code: normalizedVerificationCode,
        password: newPassword,
      });

      setNewPassword('');
      setNewPasswordConfirm('');
      setStatusMessage(null);
      setPopupState({
        level: 1,
        message: '비밀번호 변경이 완료되었습니다.',
      });
    } catch (error) {
      setErrorReasons(error instanceof RecoveryApiError ? error.reasons : ['비밀번호 재설정 중 알 수 없는 오류가 발생했습니다.']);
    } finally {
      setIsResettingPassword(false);
    }
  }

  function handlePopupConfirm() {
    setPopupState(null);
    onClose();
  }

  function moveToLoginWithUserId() {
    if (!recoveredUserId) {
      return;
    }

    navigate('/', {
      replace: true,
      state: {
        ...(window.history.state ?? {}),
        prefillLoginId: recoveredUserId,
        focusLoginPassword: true,
      },
    });
  }

  return (
    <div className="signup-overlay-layout account-recovery-overlay" id="auth-form">
      <div className="signup-close-row" data-title={overlayTitle}>
        <button
          type="button"
          className="signup-close-button"
          onClick={onClose}
          aria-label={`${overlayTitle} 닫기`}
        >
          X
        </button>
      </div>

      <section className="signup-split-layout">
        <div className="signup-guide-panel">
          <p className="panel-meta">{overlayTitle}</p>
          <div className="signup-guide-copy account-recovery-guide-copy">
            {guideLines.map((line, index) => (
              <p key={line} className={`signup-guide-message account-recovery-guide-message ${index > 0 ? 'is-compact' : ''}`}>
                {line}
              </p>
            ))}
          </div>
        </div>

        <section className="signup-card">
          <div className="signup-card-header">
            <h1 className="signup-form-title">{overlayTitle}</h1>
          </div>

          <div className="field-stack">
            <label className="field-label" htmlFor={`${mode}-email`}>
              이메일
            </label>
            <div className="inline-field-row">
              <input
                id={`${mode}-email`}
                type="email"
                className="text-field"
                value={email}
                onChange={(event) => {
                  setEmail(event.target.value);
                  resetRecoveryState();
                }}
                placeholder="이메일을 입력하세요"
                autoComplete="email"
                inputMode="email"
                aria-invalid={normalizedEmail !== '' && !isEmailValid}
              />
              <button
                type="button"
                className="btn secondary fixed-action"
                onClick={handleSendCode}
                disabled={!canSendCode}
              >
                {isSendingCode ? '전송 중...' : '코드 전송'}
              </button>
            </div>
            <p className={`hint-text signup-field-hint ${normalizedEmail !== '' && !isEmailValid ? 'is-error' : ''}`}>
              {emailHintMessage}
            </p>
          </div>

          <div className="field-stack">
            <label className="field-label" htmlFor={`${mode}-verification-code`}>
              인증코드
            </label>
            <div className="inline-field-row">
              <input
                id={`${mode}-verification-code`}
                className="text-field"
                value={verificationCode}
                onChange={(event) => {
                  setVerificationCode(sanitizeVerificationCode(event.target.value));
                  setErrorReasons([]);
                  setRecoveredUserId(null);
                  if (!isFindUserIdMode) {
                    setIsResetPasswordVerified(false);
                  }
                }}
                placeholder="인증코드 6자를 입력하세요"
                inputMode="text"
                autoComplete="one-time-code"
                maxLength={6}
                aria-invalid={verificationCode.length > 0 && !isVerificationCodeValid}
              />
              <button
                type="button"
                className="btn secondary fixed-action"
                onClick={handleVerifyCode}
                disabled={!canVerifyCode}
              >
                {isVerifyingCode ? '확인 중...' : '코드 확인'}
              </button>
            </div>
            <p className={`hint-text signup-field-hint ${errorReasons.length > 0 ? 'is-error' : statusMessage ? 'is-success' : ''}`}>
              {verificationCodeHintMessage}
            </p>
          </div>

          {isFindUserIdMode && recoveredUserId ? (
            <div className="recovery-result-box" role="status" aria-live="polite">
              <p className="recovery-result-title">가입된 아이디</p>
              <div className="recovery-result-row">
                <p className="recovery-result-value">{recoveredUserId}</p>
                <button
                  type="button"
                  className="recovery-result-arrow-button"
                  onClick={moveToLoginWithUserId}
                  aria-label="아이디를 입력한 채 로그인 화면으로 이동"
                >
                  →
                </button>
              </div>
            </div>
          ) : null}

          {!isFindUserIdMode && isResetPasswordVerified ? (
            <>
              <div className="field-stack">
                <label className="field-label" htmlFor="reset-password">
                  새 비밀번호
                </label>
                <input
                  id="reset-password"
                  type="password"
                  className="text-field"
                  value={newPassword}
                  onChange={(event) => {
                    setNewPassword(event.target.value);
                    setErrorReasons([]);
                    setPopupState(null);
                  }}
                  placeholder="새 비밀번호를 입력하세요"
                  autoComplete="new-password"
                  aria-invalid={newPassword.length > 0 && !isNewPasswordValid}
                />
                <p className={`hint-text signup-field-hint ${newPassword.length > 0 && !isNewPasswordValid ? 'is-error' : isNewPasswordValid ? 'is-success' : ''}`}>
                  {PASSWORD_HINT}
                </p>
              </div>

              <div className="field-stack">
                <label className="field-label" htmlFor="reset-password-confirm">
                  새 비밀번호 확인
                </label>
                <input
                  id="reset-password-confirm"
                  type="password"
                  className="text-field"
                  value={newPasswordConfirm}
                  onChange={(event) => {
                    setNewPasswordConfirm(event.target.value);
                    setErrorReasons([]);
                    setPopupState(null);
                  }}
                  placeholder="새 비밀번호를 다시 입력하세요"
                  autoComplete="new-password"
                  aria-invalid={newPasswordConfirm.length > 0 && !isNewPasswordConfirmValid}
                />
                <p
                  className={`hint-text signup-field-hint ${
                    newPasswordConfirm.length > 0 && !isNewPasswordConfirmValid ? 'is-error' : isNewPasswordConfirmValid ? 'is-success' : ''
                  }`}
                >
                  {PASSWORD_CONFIRM_HINT}
                </p>
              </div>

              <button
                type="button"
                className="btn primary full-width"
                onClick={handleResetPassword}
                disabled={!canResetPassword}
              >
                {isResettingPassword ? '변경 중...' : '비밀번호 변경'}
              </button>
            </>
          ) : null}

          {errorReasons.length > 0 ? (
            <div className="signup-feedback-box" role="alert" aria-live="polite">
              {errorReasons.map((reason) => (
                <p key={reason} className="signup-feedback-message">
                  {reason}
                </p>
              ))}
            </div>
          ) : null}
        </section>
      </section>

      <StatusPopup
        open={popupState !== null}
        level={popupState?.level ?? 1}
        message={popupState?.message ?? ''}
        confirmLabel="확인"
        onConfirm={handlePopupConfirm}
      />
    </div>
  );
}
