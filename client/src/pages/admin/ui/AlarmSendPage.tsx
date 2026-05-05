import { useEffect, useRef, useState } from 'react';
import { fetchAdminAlarmRecipients, sendAdminAlarm } from '@/shared/api/alarm-admin-api';
import { showSessionToast } from '@/shared/auth/session';
import { getUiTextValue, useUiText } from '@/shared/config/ui-text';

function CloseIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="m6 6 8 8M14 6l-8 8"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  );
}

export function AlarmSendContent() {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const { text } = useUiText();
  const [recipientQuery, setRecipientQuery] = useState('');
  const [selectedRecipients, setSelectedRecipients] = useState<string[]>([]);
  const [recipientOptions, setRecipientOptions] = useState<string[]>([]);
  const [isRecipientLoading, setIsRecipientLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const trimmedQuery = recipientQuery.trim();

    if (trimmedQuery === '') {
      setRecipientOptions([]);
      setIsRecipientLoading(false);
      return;
    }

    let cancelled = false;
    setIsRecipientLoading(true);

    const timeoutId = window.setTimeout(() => {
      fetchAdminAlarmRecipients(trimmedQuery)
        .then((nextRecipients) => {
          if (cancelled) {
            return;
          }

          setRecipientOptions(nextRecipients.filter((handle) => !selectedRecipients.includes(handle)));
        })
        .catch(() => {
          if (!cancelled) {
            setRecipientOptions([]);
          }
        })
        .finally(() => {
          if (!cancelled) {
            setIsRecipientLoading(false);
          }
        });
    }, 180);

    return () => {
      cancelled = true;
      window.clearTimeout(timeoutId);
    };
  }, [recipientQuery, selectedRecipients]);

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setRecipientOptions([]);
      }
    }

    window.addEventListener('mousedown', handlePointerDown);
    return () => window.removeEventListener('mousedown', handlePointerDown);
  }, []);

  function handleRecipientSelect(handle: string) {
    setSelectedRecipients((currentRecipients) => (currentRecipients.includes(handle) ? currentRecipients : [...currentRecipients, handle]));
    setRecipientQuery('');
    setRecipientOptions([]);
    setErrorMessage(null);
  }

  function handleRecipientRemove(handle: string) {
    setSelectedRecipients((currentRecipients) => currentRecipients.filter((currentHandle) => currentHandle !== handle));
    setErrorMessage(null);
  }

  async function handleSubmit() {
    if (selectedRecipients.length === 0) {
      setErrorMessage(getUiTextValue('ALARM_SEND_RECIPIENT_REQUIRED_MESSAGE', '수신자 선택은 필수입니다.'));
      return;
    }

    if (message.trim() === '') {
      setErrorMessage(getUiTextValue('ALARM_SEND_CONTENT_REQUIRED_MESSAGE', '알림 내용 입력은 필수입니다.'));
      return;
    }

    setIsSending(true);
    setErrorMessage(null);

    try {
      await sendAdminAlarm({
        recipientHandles: selectedRecipients,
        message,
      });

      showSessionToast(getUiTextValue('ALARM_SEND_SUCCESS_TOAST', '알림을 전송했습니다.'));
      setSelectedRecipients([]);
      setRecipientQuery('');
      setRecipientOptions([]);
      setMessage('');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : getUiTextValue('ALARM_SEND_FAIL_MESSAGE', '알림 전송에 실패했습니다.'));
    } finally {
      setIsSending(false);
    }
  }

  return (
    <section className="admin-alarm-send-panel">
      <div className="admin-config-toolbar" aria-hidden="true" />

      {errorMessage ? <p className="admin-config-feedback is-error">{errorMessage}</p> : null}

      <div className="admin-alarm-send-form" ref={rootRef}>
        <label className="admin-alarm-send-field">
          <span className="admin-auth-permission-label">{text('ALARM_SEND_RECIPIENT_LABEL', '수신자')}</span>
          <div className="admin-alarm-send-recipient-shell">
            <div className="admin-alarm-send-chip-list">
              {selectedRecipients.map((handle) => (
                <span key={handle} className="admin-alarm-send-chip">
                  <span>{handle}</span>
                  <button
                    type="button"
                    className="btn text admin-alarm-send-chip-remove"
                    onClick={() => handleRecipientRemove(handle)}
                    aria-label={text('ALARM_SEND_RECIPIENT_REMOVE_LABEL', { handle }, '{handle} 제거')}
                  >
                    <CloseIcon />
                  </button>
                </span>
              ))}

              <input
                type="text"
                className="text-field admin-config-input admin-alarm-send-input"
                placeholder={text('ALARM_SEND_HANDLE_PLACEHOLDER', 'Handle 검색')}
                value={recipientQuery}
                onChange={(event) => {
                  setRecipientQuery(event.target.value);
                  setErrorMessage(null);
                }}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' && recipientOptions.length > 0) {
                    event.preventDefault();
                    handleRecipientSelect(recipientOptions[0]);
                  }
                }}
              />
            </div>

            {recipientQuery.trim() !== '' ? (
              <div className="admin-alarm-send-recipient-menu">
                {isRecipientLoading ? (
                  <span className="admin-alarm-send-recipient-empty">{text('ALARM_SEND_SEARCHING_STATE', '검색 중')}</span>
                ) : recipientOptions.length > 0 ? (
                  recipientOptions.map((handle) => (
                    <button
                      key={handle}
                      type="button"
                      className="btn text admin-alarm-send-recipient-option"
                      onClick={() => handleRecipientSelect(handle)}
                    >
                      {handle}
                    </button>
                  ))
                ) : (
                  <span className="admin-alarm-send-recipient-empty">{text('ALARM_SEND_EMPTY_SEARCH_RESULT', '검색 결과가 없습니다.')}</span>
                )}
              </div>
            ) : null}
          </div>
        </label>

        <label className="admin-alarm-send-field">
          <span className="admin-auth-permission-label">{text('ALARM_SEND_CONTENT_LABEL', '내용')}</span>
          <textarea
            className="text-field admin-auth-permission-input admin-alarm-send-textarea"
            value={message}
            onChange={(event) => {
              setMessage(event.target.value);
              setErrorMessage(null);
            }}
            rows={6}
            placeholder={text('ALARM_SEND_CONTENT_PLACEHOLDER', '보낼 알림 내용을 입력해 주세요.')}
          />
        </label>

        <div className="admin-auth-save-row">
          <button
            type="button"
            className="btn secondary admin-auth-save-button"
            onClick={() => {
              void handleSubmit();
            }}
            disabled={isSending}
          >
            {isSending ? text('COMMON_SENDING_LABEL', '전송 중') : text('ALARM_SEND_BUTTON', '보내기')}
          </button>
        </div>
      </div>
    </section>
  );
}
