import { useEffect, useState, type FormEvent } from 'react';
import {
  deleteMarquee,
  fetchAdminMarquees,
  saveMarquee,
  type MarqueeItemData,
  type MarqueeSavePayload,
} from '../lib/marquee';

type MarqueeTarget = MarqueeSavePayload['targets'][number];
type MarqueeMode = MarqueeSavePayload['mode'];
type MarqueeSchedulePattern = NonNullable<MarqueeSavePayload['schedulePattern']>;

interface MarqueeDraft {
  targets: MarqueeTarget[];
  message: string;
  mode: MarqueeMode;
  startedAt: string;
  repeatCount: string;
  schedulePattern: MarqueeSchedulePattern;
  scheduleTime: string;
}

const TARGET_OPTIONS: { value: MarqueeTarget; label: string }[] = [
  { value: 'all', label: '전체' },
  { value: 'guest', label: '비회원' },
  { value: 'user', label: 'User' },
  { value: 'admin', label: 'Admin' },
  { value: 'problemGenerator', label: 'ProblemGenerator' },
];

const SCHEDULE_PATTERN_OPTIONS: { value: MarqueeSchedulePattern; label: string }[] = [
  { value: 'always', label: '상시' },
  { value: 'daily', label: '매일' },
  { value: 'weekdays', label: '평일' },
  { value: 'weekend', label: '주말' },
];

function createInitialDraft(): MarqueeDraft {
  return {
    targets: ['all'],
    message: '',
    mode: 'schedule',
    startedAt: '',
    repeatCount: '1',
    schedulePattern: 'always',
    scheduleTime: '',
  };
}

function toDraft(item: MarqueeItemData): MarqueeDraft {
  return {
    targets: item.targets,
    message: item.message,
    mode: item.mode,
    startedAt: normalizeDateTimeInputValue(item.startedAt),
    repeatCount: item.repeatCount != null ? String(item.repeatCount) : '1',
    schedulePattern: item.schedulePattern ?? 'always',
    scheduleTime: item.scheduleTime ?? '',
  };
}

function normalizeDateTimeInputValue(value: string | null) {
  return value ? value.slice(0, 16) : '';
}

function formatTargetLabel(target: MarqueeTarget) {
  return TARGET_OPTIONS.find((option) => option.value === target)?.label ?? target;
}

function formatModeLabel(mode: MarqueeMode) {
  return mode === 'repeat' ? '반복' : '스케줄';
}

function formatScheduleLabel(pattern: MarqueeSchedulePattern | null, time: string | null) {
  if (pattern == null) {
    return '-';
  }

  if (pattern === 'always') {
    return '상시';
  }

  const label = SCHEDULE_PATTERN_OPTIONS.find((option) => option.value === pattern)?.label ?? pattern;
  return time ? `${label} ${time}` : label;
}

function formatRepeatLabel(startedAt: string | null, repeatCount: number | null) {
  if (!startedAt || repeatCount == null) {
    return '-';
  }

  return `${startedAt.slice(0, 16).replace('T', ' ')} / ${repeatCount}회`;
}

function RefreshIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="M16.2 9.1a6.2 6.2 0 1 1-1.6-4.2"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.7"
      />
      <path
        d="M12.8 3.2h2.8V6"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.7"
      />
    </svg>
  );
}

function EditIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="M4.2 13.9 3.5 16.5l2.6-.7L14.7 7.2a1.5 1.5 0 0 0 0-2.1l-.8-.8a1.5 1.5 0 0 0-2.1 0Z"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
      <path d="m11.1 4.9 4 4" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.6" />
    </svg>
  );
}

function DeleteIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="M6.8 6.4v7m3.2-7v7m3.2-7v7M4.8 5.1h10.4M7.8 3.8h4.4m-6.3 1.3.5 10a1.4 1.4 0 0 0 1.4 1.3h4.4a1.4 1.4 0 0 0 1.4-1.3l.5-10"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.5"
      />
    </svg>
  );
}

function sortMarqueeItems(items: MarqueeItemData[]) {
  return [...items].sort((left, right) => right.marqueeId - left.marqueeId);
}

export function MarqueeManageContent() {
  const [marqueeItems, setMarqueeItems] = useState<MarqueeItemData[]>([]);
  const [draft, setDraft] = useState<MarqueeDraft>(createInitialDraft);
  const [editingMarqueeId, setEditingMarqueeId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [reloadSequence, setReloadSequence] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function loadMarquees() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const nextItems = await fetchAdminMarquees();

        if (cancelled) {
          return;
        }

        setMarqueeItems(sortMarqueeItems(nextItems));
      } catch (error) {
        if (cancelled) {
          return;
        }

        setErrorMessage(error instanceof Error ? error.message : '전광판 목록을 불러오지 못했다.');
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadMarquees();

    return () => {
      cancelled = true;
    };
  }, [reloadSequence]);

  function resetDraft() {
    setDraft(createInitialDraft());
    setEditingMarqueeId(null);
  }

  function handleTargetToggle(target: MarqueeTarget) {
    setDraft((currentDraft) => {
      if (target === 'all') {
        return {
          ...currentDraft,
          targets: currentDraft.targets.includes('all') ? [] : ['all'],
        };
      }

      const nextTargets = currentDraft.targets.includes('all')
        ? [target]
        : currentDraft.targets.includes(target)
          ? currentDraft.targets.filter((value) => value !== target)
          : [...currentDraft.targets, target];

      return {
        ...currentDraft,
        targets: nextTargets,
      };
    });
  }

  function handleModeChange(mode: MarqueeMode) {
    setDraft((currentDraft) => ({
      ...currentDraft,
      mode,
      schedulePattern: mode === 'schedule' ? currentDraft.schedulePattern : 'always',
      scheduleTime: mode === 'schedule' && currentDraft.schedulePattern !== 'always' ? currentDraft.scheduleTime : '',
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    setErrorMessage(null);

    try {
      await saveMarquee(
        {
          targets: draft.targets,
          message: draft.message,
          mode: draft.mode,
          startedAt: draft.mode === 'repeat' ? draft.startedAt || null : null,
          repeatCount: draft.mode === 'repeat' ? Number.parseInt(draft.repeatCount, 10) || null : null,
          schedulePattern: draft.mode === 'schedule' ? draft.schedulePattern : null,
          scheduleTime: draft.mode === 'schedule' && draft.schedulePattern !== 'always' ? draft.scheduleTime || null : null,
        },
        editingMarqueeId ?? undefined,
      );

      resetDraft();
      setReloadSequence((value) => value + 1);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '전광판을 저장하지 못했다.');
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(marqueeId: number) {
    setErrorMessage(null);

    try {
      await deleteMarquee(marqueeId);

      if (editingMarqueeId === marqueeId) {
        resetDraft();
      }

      setReloadSequence((value) => value + 1);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '전광판을 삭제하지 못했다.');
    }
  }

  return (
    <section className="panel-card admin-marquee-panel">
      <div className="admin-marquee-toolbar">
        <button
          type="button"
          className="btn text admin-marquee-refresh-button"
          onClick={() => setReloadSequence((value) => value + 1)}
          disabled={isLoading || isSaving}
          aria-label="새로고침"
          title="새로고침"
        >
          <RefreshIcon />
        </button>
      </div>

      <form className="admin-marquee-form" onSubmit={handleSubmit}>
        <label className="admin-marquee-field">
          <span className="admin-marquee-label">대상</span>
          <div className="admin-marquee-target-list">
            {TARGET_OPTIONS.map((option) => {
              const isSelected = draft.targets.includes(option.value);

              return (
                <button
                  key={option.value}
                  type="button"
                  className={`segmented-btn admin-marquee-target-button ${isSelected ? 'is-selected' : ''}`}
                  onClick={() => handleTargetToggle(option.value)}
                >
                  {option.label}
                </button>
              );
            })}
          </div>
        </label>

        <label className="admin-marquee-field">
          <span className="admin-marquee-label">문구</span>
          <textarea
            className="text-field admin-marquee-message-input"
            value={draft.message}
            onChange={(event) => setDraft((currentDraft) => ({ ...currentDraft, message: event.target.value }))}
          />
        </label>

        <div className="admin-marquee-settings-row">
          <label className="admin-marquee-field">
            <span className="admin-marquee-label">방식</span>
            <div className="admin-marquee-mode-list">
              <button
                type="button"
                className={`segmented-btn admin-marquee-mode-button ${draft.mode === 'repeat' ? 'is-selected' : ''}`}
                onClick={() => handleModeChange('repeat')}
              >
                반복
              </button>
              <button
                type="button"
                className={`segmented-btn admin-marquee-mode-button ${draft.mode === 'schedule' ? 'is-selected' : ''}`}
                onClick={() => handleModeChange('schedule')}
              >
                스케줄
              </button>
            </div>
          </label>

          {draft.mode === 'repeat' ? (
            <>
              <label className="admin-marquee-field">
                <span className="admin-marquee-label">시작</span>
                <input
                  type="datetime-local"
                  className="text-field admin-marquee-inline-input"
                  value={draft.startedAt}
                  onChange={(event) => setDraft((currentDraft) => ({ ...currentDraft, startedAt: event.target.value }))}
                />
              </label>

              <label className="admin-marquee-field">
                <span className="admin-marquee-label">횟수</span>
                <input
                  type="number"
                  min={1}
                  className="text-field admin-marquee-inline-input"
                  value={draft.repeatCount}
                  onChange={(event) => setDraft((currentDraft) => ({ ...currentDraft, repeatCount: event.target.value }))}
                />
              </label>
            </>
          ) : (
            <>
              <label className="admin-marquee-field">
                <span className="admin-marquee-label">주기</span>
                <select
                  className="text-field admin-marquee-inline-input"
                  value={draft.schedulePattern}
                  onChange={(event) =>
                    setDraft((currentDraft) => ({
                      ...currentDraft,
                      schedulePattern: event.target.value as MarqueeSchedulePattern,
                      scheduleTime: event.target.value === 'always' ? '' : currentDraft.scheduleTime,
                    }))
                  }
                >
                  {SCHEDULE_PATTERN_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>

              <label className="admin-marquee-field">
                <span className="admin-marquee-label">시간</span>
                <input
                  type="time"
                  className="text-field admin-marquee-inline-input"
                  value={draft.schedulePattern === 'always' ? '' : draft.scheduleTime}
                  onChange={(event) => setDraft((currentDraft) => ({ ...currentDraft, scheduleTime: event.target.value }))}
                  disabled={draft.schedulePattern === 'always'}
                />
              </label>
            </>
          )}

          <div className="admin-marquee-submit-row">
            <button type="submit" className="btn primary" disabled={isSaving}>
              {editingMarqueeId != null ? '수정' : '저장'}
            </button>
            {editingMarqueeId != null ? (
              <button type="button" className="btn secondary" onClick={resetDraft} disabled={isSaving}>
                취소
              </button>
            ) : null}
          </div>
        </div>
      </form>

      {errorMessage ? <p className="admin-marquee-feedback is-error">{errorMessage}</p> : null}

      {isLoading ? (
        <p className="content-text">전광판 목록을 불러오는 중이다.</p>
      ) : marqueeItems.length === 0 ? (
        <p className="admin-marquee-empty">등록된 전광판이 없다.</p>
      ) : (
        <div className="admin-marquee-table" role="table" aria-label="전광판 목록">
          <div className="admin-marquee-table-row admin-marquee-table-head" role="row">
            <span>target</span>
            <span>message</span>
            <span>mode</span>
            <span>time</span>
            <span aria-hidden="true" />
          </div>

          {marqueeItems.map((item) => (
            <div
              key={item.marqueeId}
              className={`admin-marquee-table-row ${item.active ? 'is-active' : ''}`}
              role="row"
            >
              <div className="admin-marquee-chip-list">
                {item.targets.map((target) => (
                  <span key={`${item.marqueeId}-${target}`} className="admin-marquee-chip">
                    {formatTargetLabel(target)}
                  </span>
                ))}
              </div>

              <span className="admin-marquee-table-message">{item.message}</span>
              <span className="admin-marquee-table-mode">{formatModeLabel(item.mode)}</span>
              <span className="admin-marquee-table-time">
                {item.mode === 'repeat'
                  ? formatRepeatLabel(item.startedAt, item.repeatCount)
                  : formatScheduleLabel(item.schedulePattern, item.scheduleTime)}
              </span>

              <div className="admin-marquee-actions">
                <button
                  type="button"
                  className="btn text admin-config-icon-button"
                  onClick={() => {
                    setDraft(toDraft(item));
                    setEditingMarqueeId(item.marqueeId);
                    setErrorMessage(null);
                  }}
                  aria-label="수정"
                  title="수정"
                >
                  <EditIcon />
                </button>
                <button
                  type="button"
                  className="btn text admin-config-icon-button admin-config-delete-button"
                  onClick={() => void handleDelete(item.marqueeId)}
                  aria-label="삭제"
                  title="삭제"
                >
                  <DeleteIcon />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
