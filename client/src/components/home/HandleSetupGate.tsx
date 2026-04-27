import { useEffect, useState } from 'react';
import { AuthApiError, SignupApiError, setupHandle } from '../../lib/authApi';
import { completeAuthentication } from '../../lib/authSession';
import { useMockSession } from '../../lib/session';
import { useUiText } from '../../lib/uiText';

const HANDLE_PATTERN = /^[A-Za-z0-9_-]{1,15}$/;

function sanitizeHandle(value: string) {
  return value.replace(/[^A-Za-z0-9_-]/g, '').slice(0, 15);
}

export default function HandleSetupGate() {
  const { text } = useUiText();
  const { handleSetupRequired } = useMockSession();
  const [handleValue, setHandleValue] = useState('');
  const [errorReasons, setErrorReasons] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const duplicatedHandleReason = text('HANDLE_DUPLICATED_MESSAGE', '이미 사용 중인 Handle입니다.');
  const handleHint = text('HANDLE_HINT_MESSAGE', '영문, 숫자, 언더스코어(_)와 하이픈(-)만 사용할 수 있으며 최대 15자까지 입력할 수 있습니다.');
  const handleNotice = text('HANDLE_NOTICE_MESSAGE', 'Handle은 다른 사용자에게 보이는 이름입니다. 한번 설정하면 변경할 수 없습니다.');
  const normalizedHandle = handleValue.trim();
  const isHandleValid = HANDLE_PATTERN.test(normalizedHandle);
  const isDuplicatedHandle = errorReasons.includes(duplicatedHandleReason);
  const hasHandleError = normalizedHandle !== '' && !isHandleValid;
  const inlineHintMessage = isDuplicatedHandle ? duplicatedHandleReason : handleHint;
  const remainingErrorReasons = errorReasons.filter((reason) => reason !== duplicatedHandleReason);
  const canSubmit = isHandleValid && !isSubmitting;

  useEffect(() => {
    if (typeof document === 'undefined') {
      return;
    }

    document.body.classList.toggle('handle-setup-locked', handleSetupRequired);

    return () => {
      document.body.classList.remove('handle-setup-locked');
    };
  }, [handleSetupRequired]);

  useEffect(() => {
    if (handleSetupRequired) {
      return;
    }

    setHandleValue('');
    setErrorReasons([]);
    setIsSubmitting(false);
  }, [handleSetupRequired]);

  if (!handleSetupRequired) {
    return null;
  }

  function applySetupErrorReasons(reasons: string[]) {
    for (const reason of reasons) {
      if (reason.includes('Handle')) {
        setErrorReasons([duplicatedHandleReason]);
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

      const session = await setupHandle({
        handle: normalizedHandle,
      });

      await completeAuthentication(session);
    } catch (error) {
      if (error instanceof SignupApiError || error instanceof AuthApiError) {
        applySetupErrorReasons(error.reasons);
        return;
      }

      setErrorReasons([error instanceof Error ? error.message : text('HANDLE_SETUP_FAIL_MESSAGE', 'Handle 설정 중 오류가 발생했습니다.')]);
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
            {text('HANDLE_SETUP_TITLE', 'Handle 설정')}
          </h1>
          <p className="handle-setup-copy">{handleNotice}</p>
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
            placeholder={text('HANDLE_PLACEHOLDER', '사용할 Handle을 입력하세요')}
            autoComplete="username"
            maxLength={15}
            aria-label={text('COMMON_HANDLE_LABEL', 'Handle')}
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
          {isSubmitting ? text('COMMON_PROCESSING_LABEL', '처리 중...') : text('HANDLE_COMPLETE_BUTTON', 'Handle 설정 완료')}
        </button>
      </section>
    </div>
  );
}
