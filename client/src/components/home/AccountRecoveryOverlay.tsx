import { useRef, useState, type RefObject } from 'react';
import StatusPopup from '../common/StatusPopup';
import { RecoveryApiError, resetPassword, sendPasswordResetCode, verifyPasswordResetCode } from '../../lib/authApi';
import { useUiText } from '../../lib/uiText';

type PopupLevel = 1 | 2 | 3;

interface AccountRecoveryOverlayProps {
  onClose: () => void;
}

interface PopupState {
  level: PopupLevel;
  message: string;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const VERIFICATION_CODE_PATTERN = /^[A-Z0-9]{6}$/;

function sanitizeVerificationCode(value: string) {
  return value.replace(/[^A-Za-z0-9]/g, '').toUpperCase().slice(0, 6);
}

function hasRequiredPasswordFormat(value: string) {
  return value.length >= 8 && /[^A-Za-z0-9]/.test(value);
}

export default function AccountRecoveryOverlay({ onClose }: AccountRecoveryOverlayProps) {
  const { text } = useUiText();
  const verificationCodeInputRef = useRef<HTMLInputElement | null>(null);
  const newPasswordInputRef = useRef<HTMLInputElement | null>(null);
  const newPasswordConfirmInputRef = useRef<HTMLInputElement | null>(null);
  const [email, setEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [errorReasons, setErrorReasons] = useState<string[]>([]);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [isCodeSent, setIsCodeSent] = useState(false);
  const [isResetPasswordVerified, setIsResetPasswordVerified] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isVerifyingCode, setIsVerifyingCode] = useState(false);
  const [isResettingPassword, setIsResettingPassword] = useState(false);
  const [popupState, setPopupState] = useState<PopupState | null>(null);
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
  const overlayTitle = text('AUTH_RESET_TITLE', '비밀번호 찾기');
  const closeButtonLabel = text('COMMON_CLOSE_BUTTON', '닫기');
  const confirmButtonLabel = text('COMMON_CONFIRM_BUTTON', '확인');
  const emailLabel = text('AUTH_EMAIL_LABEL', '이메일');
  const codeLabel = text('AUTH_CODE_LABEL', '인증 코드');
  const newPasswordLabel = text('AUTH_NEW_PASSWORD_LABEL', '새 비밀번호');
  const newPasswordConfirmLabel = text('AUTH_NEW_PASSWORD_CONFIRM_LABEL', '새 비밀번호 확인');
  const codeSentMessage = text('AUTH_CODE_SENT_MESSAGE', '인증 코드를 전송했습니다. 5분 이내에 입력해 주세요.');
  const codeVerifiedMessage = text('AUTH_RESET_CODE_VERIFIED_MESSAGE', '인증 코드가 확인되었습니다. 새 비밀번호를 입력해 주세요.');
  const verificationCodeHint = text('RECOVERY_CODE_HINT', '이메일로 받은 인증코드 6자를 5분 내에 입력해 주세요.');
  const passwordHint = text('RECOVERY_PASSWORD_HINT', '비밀번호는 특수문자를 포함해 8자 이상이어야 합니다.');
  const passwordConfirmHint = text('RECOVERY_PASSWORD_CONFIRM_HINT', '비밀번호 확인은 비밀번호와 동일하게 입력해 주세요.');
  const resetPasswordGuideLines = [
    text('RECOVERY_GUIDE_EMAIL', '가입할 때 사용한 이메일을 입력하면 인증코드를 보내드립니다.'),
    text('RECOVERY_GUIDE_RESET', '인증코드 확인이 완료되면 새로운 비밀번호를 바로 설정할 수 있습니다.'),
  ];
  const isResetPasswordStage = isResetPasswordVerified;
  const emailHintMessage =
    normalizedEmail === ''
      ? text('RECOVERY_EMAIL_REQUIRED_MESSAGE', '가입할 때 사용한 이메일을 입력해 주세요.')
      : !isEmailValid
        ? text('AUTH_EMAIL_HINT', '올바른 이메일 형식으로 입력해 주세요.')
        : text('RECOVERY_EMAIL_VALID_MESSAGE', '인증코드를 받을 수 있는 이메일입니다.');
  const emailHintMessageAfterSendCode = statusMessage === codeSentMessage ? codeSentMessage : emailHintMessage;
  const verificationCodeHintMessage = statusMessage && statusMessage !== codeSentMessage ? statusMessage : verificationCodeHint;
  const isVerificationCodeStatusSuccess = statusMessage !== null && statusMessage !== codeSentMessage;

  function resetRecoveryState() {
    setVerificationCode('');
    setNewPassword('');
    setNewPasswordConfirm('');
    setErrorReasons([]);
    setStatusMessage(null);
    setIsCodeSent(false);
    setIsResetPasswordVerified(false);
    setPopupState(null);
  }

  async function handleSendCode() {
    if (!canSendCode) {
      return false;
    }

    try {
      setIsSendingCode(true);
      setErrorReasons([]);
      setStatusMessage(null);
      setIsResetPasswordVerified(false);
      setPopupState(null);
      await sendPasswordResetCode({ email: normalizedEmail });
      setIsCodeSent(true);
      setVerificationCode('');
      setStatusMessage(codeSentMessage);
      return true;
    } catch (error) {
      setErrorReasons(error instanceof RecoveryApiError ? error.reasons : [text('RECOVERY_UNKNOWN_SEND_FAIL_MESSAGE', '인증코드 발송 중 알 수 없는 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsSendingCode(false);
    }
  }

  async function handleVerifyCode() {
    if (!canVerifyCode) {
      return false;
    }

    try {
      setIsVerifyingCode(true);
      setErrorReasons([]);
      setStatusMessage(null);
      await verifyPasswordResetCode({
        email: normalizedEmail,
        code: normalizedVerificationCode,
      });
      setIsResetPasswordVerified(true);
      setStatusMessage(codeVerifiedMessage);
      return true;
    } catch (error) {
      setIsResetPasswordVerified(false);
      setErrorReasons(error instanceof RecoveryApiError ? error.reasons : [text('RECOVERY_UNKNOWN_VERIFY_FAIL_MESSAGE', '인증코드 확인 중 알 수 없는 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsVerifyingCode(false);
    }
  }

  async function handleResetPassword() {
    if (!canResetPassword) {
      return false;
    }

    try {
      setIsResettingPassword(true);
      setErrorReasons([]);

      await resetPassword({
        email: normalizedEmail,
        password: newPassword,
      });

      setNewPassword('');
      setNewPasswordConfirm('');
      setStatusMessage(null);
      setPopupState({
        level: 1,
        message: text('RECOVERY_RESET_SUCCESS_MESSAGE', '비밀번호 변경이 완료되었습니다.'),
      });
      return true;
    } catch (error) {
      setErrorReasons(error instanceof RecoveryApiError ? error.reasons : [text('RECOVERY_UNKNOWN_RESET_FAIL_MESSAGE', '비밀번호 재설정 중 알 수 없는 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsResettingPassword(false);
    }
  }

  function focusNextInput(nextInputRef: RefObject<HTMLInputElement | null>) {
    window.requestAnimationFrame(() => {
      nextInputRef.current?.focus();
    });
  }

  function handlePopupConfirm() {
    setPopupState(null);
    onClose();
  }

  return (
    <div
      className={`signup-overlay-layout account-recovery-overlay ${isResetPasswordStage ? 'is-reset-password-stage' : ''}`}
      id="auth-form"
    >
      <div className="signup-close-row" data-title={overlayTitle}>
        <button
          type="button"
          className="signup-close-button"
          onClick={onClose}
          aria-label={`${overlayTitle} ${closeButtonLabel}`}
        >
          X
        </button>
      </div>

      <section className="signup-split-layout">
        <div className="signup-guide-panel">
          <p className="panel-meta">{overlayTitle}</p>
          <div className="signup-guide-copy account-recovery-guide-copy">
            {resetPasswordGuideLines.map((line, index) => (
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
            <label className="field-label" htmlFor="reset-password-email">
              {emailLabel}
            </label>
            <div className="inline-field-row">
              <input
                id="reset-password-email"
                type="email"
                className="text-field"
                onKeyDown={(event) => {
                  if (event.key !== 'Enter') {
                    return;
                  }

                  event.preventDefault();
                  void (async () => {
                    const isCodeSent = await handleSendCode();
                    if (isCodeSent) {
                      focusNextInput(verificationCodeInputRef);
                    }
                  })();
                }}
                value={email}
                onChange={(event) => {
                  setEmail(event.target.value);
                  resetRecoveryState();
                }}
                placeholder={text('AUTH_RESET_EMAIL_PLACEHOLDER', '가입한 이메일을 입력해 주세요.')}
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
                {isSendingCode ? text('COMMON_SENDING_LABEL', '전송 중') : text('AUTH_CODE_SEND_BUTTON', '코드 전송')}
              </button>
            </div>
            <p
              className={`hint-text signup-field-hint ${
                normalizedEmail !== '' && !isEmailValid ? 'is-error' : statusMessage === codeSentMessage ? 'is-success' : ''
              }`}
            >
              {emailHintMessageAfterSendCode}
            </p>
          </div>

          <div className="field-stack">
            <label className="field-label" htmlFor="reset-password-verification-code">
              {codeLabel}
            </label>
            <div className="inline-field-row">
              <input
                id="reset-password-verification-code"
                className="text-field"
                ref={verificationCodeInputRef}
                value={verificationCode}
                onChange={(event) => {
                  setVerificationCode(sanitizeVerificationCode(event.target.value));
                  setErrorReasons([]);
                  setIsResetPasswordVerified(false);
                }}
                onKeyDown={(event) => {
                  if (event.key !== 'Enter') {
                    return;
                  }

                  event.preventDefault();
                  void (async () => {
                    const isCodeVerified = await handleVerifyCode();
                    if (isCodeVerified) {
                      focusNextInput(newPasswordInputRef);
                    }
                  })();
                }}
                placeholder={text('RECOVERY_CODE_PLACEHOLDER', '인증코드 6자를 입력하세요')}
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
                {isVerifyingCode ? text('COMMON_VERIFYING_LABEL', '확인 중') : text('AUTH_CODE_VERIFY_BUTTON', '코드 확인')}
              </button>
            </div>
            <p className={`hint-text signup-field-hint ${errorReasons.length > 0 ? 'is-error' : isVerificationCodeStatusSuccess ? 'is-success' : ''}`}>
              {verificationCodeHintMessage}
            </p>
          </div>

          {isResetPasswordVerified ? (
            <>
              <div className="field-stack">
                <label className="field-label" htmlFor="reset-password">
                  {newPasswordLabel}
                </label>
                <input
                  id="reset-password"
                  type="password"
                  className="text-field"
                  ref={newPasswordInputRef}
                  value={newPassword}
                  onChange={(event) => {
                    setNewPassword(event.target.value);
                    setErrorReasons([]);
                    setPopupState(null);
                  }}
                  onKeyDown={(event) => {
                    if (event.key !== 'Enter') {
                      return;
                    }

                    event.preventDefault();
                    focusNextInput(newPasswordConfirmInputRef);
                  }}
                  placeholder={text('RECOVERY_NEW_PASSWORD_PLACEHOLDER', '새 비밀번호를 입력하세요')}
                  autoComplete="new-password"
                  aria-invalid={newPassword.length > 0 && !isNewPasswordValid}
                />
                <p className={`hint-text signup-field-hint ${newPassword.length > 0 && !isNewPasswordValid ? 'is-error' : isNewPasswordValid ? 'is-success' : ''}`}>
                  {passwordHint}
                </p>
              </div>

              <div className="field-stack">
                <label className="field-label" htmlFor="reset-password-confirm">
                  {newPasswordConfirmLabel}
                </label>
                <input
                  id="reset-password-confirm"
                  type="password"
                  className="text-field"
                  ref={newPasswordConfirmInputRef}
                  value={newPasswordConfirm}
                  onChange={(event) => {
                    setNewPasswordConfirm(event.target.value);
                    setErrorReasons([]);
                    setPopupState(null);
                  }}
                  onKeyDown={(event) => {
                    if (event.key !== 'Enter') {
                      return;
                    }

                    event.preventDefault();
                    void handleResetPassword();
                  }}
                  placeholder={text('RECOVERY_NEW_PASSWORD_CONFIRM_PLACEHOLDER', '새 비밀번호를 다시 입력하세요')}
                  autoComplete="new-password"
                  aria-invalid={newPasswordConfirm.length > 0 && !isNewPasswordConfirmValid}
                />
                <p
                  className={`hint-text signup-field-hint ${
                    newPasswordConfirm.length > 0 && !isNewPasswordConfirmValid ? 'is-error' : isNewPasswordConfirmValid ? 'is-success' : ''
                  }`}
                >
                  {passwordConfirmHint}
                </p>
              </div>

              <button
                type="button"
                className="btn primary full-width"
                onClick={handleResetPassword}
                disabled={!canResetPassword}
              >
                {isResettingPassword ? text('AUTH_PASSWORD_CHANGING_LABEL', '변경 중') : text('AUTH_PASSWORD_CHANGE_BUTTON', '비밀번호 변경')}
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
        confirmLabel={confirmButtonLabel}
        onConfirm={handlePopupConfirm}
      />
    </div>
  );
}
