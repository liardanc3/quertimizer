import { useEffect, useRef, useState } from 'react';
import { fetchAdminAlarmRecipients, sendAdminAlarm } from '../lib/alarmAdminApi';
import { showSessionToast } from '../lib/session';

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
      setErrorMessage('수신자가 필요하다.');
      return;
    }

    if (message.trim() === '') {
      setErrorMessage('알람 내용이 필요하다.');
      return;
    }

    setIsSending(true);
    setErrorMessage(null);

    try {
      await sendAdminAlarm({
        recipientHandles: selectedRecipients,
        message,
      });

      showSessionToast('알람 전송 완료.');
      setSelectedRecipients([]);
      setRecipientQuery('');
      setRecipientOptions([]);
      setMessage('');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '알람 전송에 실패했다.');
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
          <span className="admin-auth-permission-label">수신자</span>
          <div className="admin-alarm-send-recipient-shell">
            <div className="admin-alarm-send-chip-list">
              {selectedRecipients.map((handle) => (
                <span key={handle} className="admin-alarm-send-chip">
                  <span>{handle}</span>
                  <button
                    type="button"
                    className="btn text admin-alarm-send-chip-remove"
                    onClick={() => handleRecipientRemove(handle)}
                    aria-label={`${handle} 제거`}
                  >
                    <CloseIcon />
                  </button>
                </span>
              ))}

              <input
                type="text"
                className="text-field admin-config-input admin-alarm-send-input"
                placeholder="Handle 검색"
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
                  <span className="admin-alarm-send-recipient-empty">검색 중</span>
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
                  <span className="admin-alarm-send-recipient-empty">검색 결과가 없다.</span>
                )}
              </div>
            ) : null}
          </div>
        </label>

        <label className="admin-alarm-send-field">
          <span className="admin-auth-permission-label">내용</span>
          <textarea
            className="text-field admin-auth-permission-input admin-alarm-send-textarea"
            value={message}
            onChange={(event) => {
              setMessage(event.target.value);
              setErrorMessage(null);
                }}
            rows={6}
            placeholder="보낼 알람 내용을 입력"
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
            {isSending ? '전송 중' : '보내기'}
          </button>
        </div>
      </div>
    </section>
  );
}
