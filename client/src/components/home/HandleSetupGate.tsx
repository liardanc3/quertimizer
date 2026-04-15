import { useEffect, useState } from 'react';
import { AuthApiError, SignupApiError, setupUserId } from '../../lib/authApi';
import { completeAuthentication } from '../../lib/authSession';
import { useMockSession } from '../../lib/session';

const HANDLE_PATTERN = /^[A-Za-z0-9_-]{1,15}$/;
const HANDLE_HINT = '영문, 숫자, 언더스코어(_)와 하이픈(-)만 사용할 수 있으며 최대 15자까지 입력할 수 있습니다.';
const HANDLE_NOTICE = 'Handle은 타인에게 노출되는 아이디입니다. 한번 설정 시 변경할 수 없습니다.';
const DUPLICATED_HANDLE_REASON = '사용중인 Handle 입니다.';

function sanitizeHandle(value: string) {
  return value.replace(/[^A-Za-z0-9_-]/g, '').slice(0, 15);
}

export default function HandleSetupGate() {
  const { userIdSetupRequired } = useMockSession();
  const [handleValue, setHandleValue] = useState('');
  const [errorReasons, setErrorReasons] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const normalizedHandle = handleValue.trim();
  const isHandleValid = HANDLE_PATTERN.test(normalizedHandle);
  const isDuplicatedHandle = errorReasons.includes(DUPLICATED_HANDLE_REASON);
  const hasHandleError = normalizedHandle !== '' && !isHandleValid;
  const inlineHintMessage = isDuplicatedHandle ? DUPLICATED_HANDLE_REASON : HANDLE_HINT;
  const remainingErrorReasons = errorReasons.filter((reason) => reason !== DUPLICATED_HANDLE_REASON);
  const canSubmit = isHandleValid && !isSubmitting;

  useEffect(() => {
    if (typeof document === 'undefined') {
      return;
    }

    document.body.classList.toggle('handle-setup-locked', userIdSetupRequired);

    return () => {
      document.body.classList.remove('handle-setup-locked');
    };
  }, [userIdSetupRequired]);

  useEffect(() => {
    if (userIdSetupRequired) {
      return;
    }

    setHandleValue('');
    setErrorReasons([]);
    setIsSubmitting(false);
  }, [userIdSetupRequired]);

  if (!userIdSetupRequired) {
    return null;
  }

  function applySetupErrorReasons(reasons: string[]) {
    for (const reason of reasons) {
      if (reason.includes('아이디')) {
        setErrorReasons([DUPLICATED_HANDLE_REASON]);
        return;
      }
    }

    setErrorReasons(reasons);
  }

  async function handleSubmit() {
    if (!canSubmit) {
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorReasons([]);

      const session = await setupUserId({
        userId: normalizedHandle,
      });

      await completeAuthentication(session);
    } catch (error) {
      if (error instanceof SignupApiError || error instanceof AuthApiError) {
        applySetupErrorReasons(error.reasons);
        return;
      }

      setErrorReasons([error instanceof Error ? error.message : 'Handle 설정 중 오류가 발생했습니다.']);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="handle-setup-overlay" role="dialog" aria-modal="true" aria-labelledby="handle-setup-title">
      <div className="handle-setup-backdrop" aria-hidden="true" />
      <section className="handle-setup-dialog">
        <div className="handle-setup-header">
          <h1 id="handle-setup-title" className="handle-setup-title">
            Handle 설정
          </h1>
          <p className="handle-setup-copy">{HANDLE_NOTICE}</p>
        </div>

        <div className="field-stack handle-setup-field-stack">
          <input
            id="handle-setup-input"
            className="text-field handle-setup-input"
            value={handleValue}
            onChange={(event) => {
              setHandleValue(sanitizeHandle(event.target.value));
              setErrorReasons([]);
            }}
            onKeyDown={(event) => {
              if (event.key !== 'Enter') {
                return;
              }

              event.preventDefault();
              void handleSubmit();
            }}
            placeholder="사용할 Handle을 입력하세요"
            autoComplete="username"
            maxLength={15}
            aria-label="Handle 입력"
            aria-invalid={hasHandleError || isDuplicatedHandle}
            autoFocus
          />
          <p className={`hint-text handle-setup-hint ${hasHandleError || isDuplicatedHandle ? 'is-error' : ''}`}>
            {inlineHintMessage}
          </p>
        </div>

        {remainingErrorReasons.length > 0 ? (
          <div className="signup-feedback-box handle-setup-feedback" role="alert" aria-live="polite">
            {remainingErrorReasons.map((reason) => (
              <p key={reason} className="signup-feedback-message">
                {reason}
              </p>
            ))}
          </div>
        ) : null}

        <button
          type="button"
          className="btn primary full-width handle-setup-submit"
          onClick={() => void handleSubmit()}
          disabled={!canSubmit}
        >
          {isSubmitting ? '처리 중...' : 'Handle 설정 완료'}
        </button>
      </section>
    </div>
  );
}
