import { useEffect, useMemo, useState } from 'react';
import HttpErrorState from '../components/common/HttpErrorState';
import { LoadingOverlay } from '../components/common/LoadingSpinner';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { getApiErrorStatus, isCommonHttpErrorStatus } from '../lib/apiError';
import {
  blockAdminUser,
  fetchAdminAnomalyTrends,
  fetchAdminBlockedIps,
  fetchAdminBlockedUsers,
  unblockAdminIp,
  unblockAdminUser,
  type AdminAnomalyRange,
  type AdminAnomalyTrendItem,
  type AdminAnomalyTrendPageData,
  type AdminBlockedIpItem,
  type AdminBlockedIpPageData,
  type AdminBlockedUserItem,
  type AdminBlockedUserPageData,
} from '../lib/adminAnomalyApi';
import { useUiText } from '../lib/uiText';
import './SubmitHistoryPage.css';
import './AnomalyManagePage.css';

type AnomalySection = 'trend' | 'blockedUsers' | 'blockedIps';

const ADMIN_ANOMALY_PAGE_SIZE = 10;
const emptyTrendPage: AdminAnomalyTrendPageData = {
  currentPage: 1,
  pageSize: ADMIN_ANOMALY_PAGE_SIZE,
  totalCount: 0,
  totalPages: 1,
  items: [],
};
const emptyBlockedUserPage: AdminBlockedUserPageData = {
  currentPage: 1,
  pageSize: ADMIN_ANOMALY_PAGE_SIZE,
  totalCount: 0,
  totalPages: 1,
  items: [],
};
const emptyBlockedIpPage: AdminBlockedIpPageData = {
  currentPage: 1,
  pageSize: ADMIN_ANOMALY_PAGE_SIZE,
  totalCount: 0,
  totalPages: 1,
  items: [],
};
const anomalyLoadingRows = Array.from({ length: 6 }, (_, index) => index);

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  const year = String(date.getFullYear());
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

function formatDateTimeInputValue(date: Date) {
  const year = String(date.getFullYear());
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function parseDateTimeInputValue(value: string) {
  const matched = value.trim().match(/^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})$/);
  if (!matched) {
    return null;
  }

  const [, year, month, day, hour, minute] = matched;
  const parsedDate = new Date(
    Number.parseInt(year, 10),
    Number.parseInt(month, 10) - 1,
    Number.parseInt(day, 10),
    Number.parseInt(hour, 10),
    Number.parseInt(minute, 10),
  );

  if (
    Number.isNaN(parsedDate.getTime()) ||
    parsedDate.getFullYear() !== Number.parseInt(year, 10) ||
    parsedDate.getMonth() !== Number.parseInt(month, 10) - 1 ||
    parsedDate.getDate() !== Number.parseInt(day, 10) ||
    parsedDate.getHours() !== Number.parseInt(hour, 10) ||
    parsedDate.getMinutes() !== Number.parseInt(minute, 10)
  ) {
    return null;
  }

  return parsedDate;
}

function BanIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <circle cx="10" cy="10" r="6.4" fill="none" stroke="currentColor" strokeWidth="1.7" />
      <path d="M6 14 14 6" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.7" />
    </svg>
  );
}

function UnblockIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="M6.2 9.8a3.8 3.8 0 1 1 1.1 2.7" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
      <path d="M4.3 6.7v3.2h3.2" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
    </svg>
  );
}

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  draft: string;
  isEditing: boolean;
  navigationLabel: string;
  inputLabel: string;
  previousLabel: string;
  nextLabel: string;
  onDraftChange: (value: string) => void;
  onStartEditing: () => void;
  onCancelEditing: () => void;
  onApplyEditing: () => void;
  onPageChange: (page: number) => void;
}

function Pagination({
  currentPage,
  totalPages,
  draft,
  isEditing,
  navigationLabel,
  inputLabel,
  previousLabel,
  nextLabel,
  onDraftChange,
  onStartEditing,
  onCancelEditing,
  onApplyEditing,
  onPageChange,
}: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <div className="problem-pagination submit-history-pagination" role="navigation" aria-label={navigationLabel}>
      <button
        type="button"
        className="mini-toggle problem-page-button"
        onClick={() => onPageChange(Math.max(1, currentPage - 1))}
        disabled={currentPage === 1}
      >
        {previousLabel}
      </button>

      {isEditing ? (
        <input
          type="text"
          inputMode="numeric"
          pattern="[0-9]*"
          className="problem-pagination-meta-input"
          value={draft}
          onChange={(event) => onDraftChange(event.target.value.replace(/\D+/g, ''))}
          onBlur={onApplyEditing}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault();
              onApplyEditing();
            }

            if (event.key === 'Escape') {
              event.preventDefault();
              onCancelEditing();
            }
          }}
          aria-label={inputLabel}
          autoFocus
        />
      ) : (
        <button type="button" className="problem-pagination-meta problem-pagination-meta-button" onClick={onStartEditing}>
          {`${currentPage} / ${totalPages}`}
        </button>
      )}

      <button
        type="button"
        className="mini-toggle problem-page-button"
        onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
        disabled={currentPage >= totalPages}
      >
        {nextLabel}
      </button>
    </div>
  );
}

export function AnomalyManageContent() {
  const { text } = useUiText();
  const initialCustomEnd = formatDateTimeInputValue(new Date());
  const initialCustomStart = formatDateTimeInputValue(new Date(Date.now() - 24 * 60 * 60 * 1000));
  const anomalySections: Array<{ id: AnomalySection; label: string }> = useMemo(
    () => [
      { id: 'trend', label: text('ANOMALY_TREND_TAB_LABEL', '실행/제출 추이') },
      { id: 'blockedUsers', label: text('ANOMALY_BLOCKED_USERS_TAB_LABEL', '차단 계정 목록') },
      { id: 'blockedIps', label: text('ANOMALY_BLOCKED_IPS_TAB_LABEL', '차단 IP 목록') },
    ],
    [text],
  );
  const anomalyRanges: Array<{ value: AdminAnomalyRange; label: string }> = useMemo(
    () => [
      { value: '10m', label: text('ANOMALY_RANGE_10M_LABEL', '최근 10분') },
      { value: '1h', label: text('ANOMALY_RANGE_1H_LABEL', '최근 1시간') },
      { value: '24h', label: text('ANOMALY_RANGE_24H_LABEL', '최근 24시간') },
      { value: 'all', label: text('ANOMALY_RANGE_ALL_LABEL', '전체') },
      { value: 'custom', label: text('ANOMALY_RANGE_CUSTOM_LABEL', '사용자 지정') },
    ],
    [text],
  );
  const [activeSection, setActiveSection] = useState<AnomalySection>('trend');
  const [activeRange, setActiveRange] = useState<AdminAnomalyRange>('10m');
  const [customRangeStartDraft, setCustomRangeStartDraft] = useState(initialCustomStart);
  const [customRangeEndDraft, setCustomRangeEndDraft] = useState(initialCustomEnd);
  const [customRangeStart, setCustomRangeStart] = useState(initialCustomStart);
  const [customRangeEnd, setCustomRangeEnd] = useState(initialCustomEnd);
  const [customRangeErrorMessage, setCustomRangeErrorMessage] = useState<string | null>(null);
  const [trendPage, setTrendPage] = useState(1);
  const [trendPageDraft, setTrendPageDraft] = useState('1');
  const [isTrendPageEditing, setIsTrendPageEditing] = useState(false);
  const [trendPageData, setTrendPageData] = useState<AdminAnomalyTrendPageData>(emptyTrendPage);
  const [isTrendLoading, setIsTrendLoading] = useState(true);
  const [trendErrorMessage, setTrendErrorMessage] = useState<string | null>(null);
  const [trendErrorStatus, setTrendErrorStatus] = useState<number | null>(null);
  const [blockedUserPage, setBlockedUserPage] = useState(1);
  const [blockedUserPageDraft, setBlockedUserPageDraft] = useState('1');
  const [isBlockedUserPageEditing, setIsBlockedUserPageEditing] = useState(false);
  const [blockedUserPageData, setBlockedUserPageData] = useState<AdminBlockedUserPageData>(emptyBlockedUserPage);
  const [isBlockedUserLoading, setIsBlockedUserLoading] = useState(false);
  const [blockedUserErrorMessage, setBlockedUserErrorMessage] = useState<string | null>(null);
  const [blockedUserErrorStatus, setBlockedUserErrorStatus] = useState<number | null>(null);
  const [blockedIpPage, setBlockedIpPage] = useState(1);
  const [blockedIpPageDraft, setBlockedIpPageDraft] = useState('1');
  const [isBlockedIpPageEditing, setIsBlockedIpPageEditing] = useState(false);
  const [blockedIpPageData, setBlockedIpPageData] = useState<AdminBlockedIpPageData>(emptyBlockedIpPage);
  const [isBlockedIpLoading, setIsBlockedIpLoading] = useState(false);
  const [blockedIpErrorMessage, setBlockedIpErrorMessage] = useState<string | null>(null);
  const [blockedIpErrorStatus, setBlockedIpErrorStatus] = useState<number | null>(null);
  const [actingKey, setActingKey] = useState<string | null>(null);
  const [trendReloadSequence] = useState(0);
  const [blockedUserReloadSequence, setBlockedUserReloadSequence] = useState(0);
  const [blockedIpReloadSequence, setBlockedIpReloadSequence] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setIsTrendLoading(true);
    setTrendErrorMessage(null);
    setTrendErrorStatus(null);

    fetchAdminAnomalyTrends(
      activeRange,
      trendPage,
      ADMIN_ANOMALY_PAGE_SIZE,
      activeRange === 'custom' ? customRangeStart : undefined,
      activeRange === 'custom' ? customRangeEnd : undefined,
    )
      .then((nextTrendPageData) => {
        if (cancelled) {
          return;
        }

        setTrendPageData(nextTrendPageData);
        if (nextTrendPageData.currentPage !== trendPage) {
          setTrendPage(nextTrendPageData.currentPage);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setTrendErrorMessage(error instanceof Error ? error.message : text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
          const status = getApiErrorStatus(error);
          setTrendErrorStatus(isCommonHttpErrorStatus(status) ? status : null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsTrendLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [activeRange, customRangeEnd, customRangeStart, trendPage, trendReloadSequence]);

  useEffect(() => {
    if (activeSection !== 'blockedUsers' && blockedUserPageData.items.length > 0 && blockedUserReloadSequence === 0) {
      return;
    }

    let cancelled = false;
    setIsBlockedUserLoading(true);
    setBlockedUserErrorMessage(null);
    setBlockedUserErrorStatus(null);

    fetchAdminBlockedUsers(blockedUserPage, ADMIN_ANOMALY_PAGE_SIZE)
      .then((nextBlockedUserPageData) => {
        if (cancelled) {
          return;
        }

        setBlockedUserPageData(nextBlockedUserPageData);
        if (nextBlockedUserPageData.currentPage !== blockedUserPage) {
          setBlockedUserPage(nextBlockedUserPageData.currentPage);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setBlockedUserErrorMessage(error instanceof Error ? error.message : text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
          const status = getApiErrorStatus(error);
          setBlockedUserErrorStatus(isCommonHttpErrorStatus(status) ? status : null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsBlockedUserLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [activeSection, blockedUserPage, blockedUserReloadSequence, blockedUserPageData.items.length]);

  useEffect(() => {
    if (activeSection !== 'blockedIps' && blockedIpPageData.items.length > 0 && blockedIpReloadSequence === 0) {
      return;
    }

    let cancelled = false;
    setIsBlockedIpLoading(true);
    setBlockedIpErrorMessage(null);
    setBlockedIpErrorStatus(null);

    fetchAdminBlockedIps(blockedIpPage, ADMIN_ANOMALY_PAGE_SIZE)
      .then((nextBlockedIpPageData) => {
        if (cancelled) {
          return;
        }

        setBlockedIpPageData(nextBlockedIpPageData);
        if (nextBlockedIpPageData.currentPage !== blockedIpPage) {
          setBlockedIpPage(nextBlockedIpPageData.currentPage);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setBlockedIpErrorMessage(error instanceof Error ? error.message : text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
          const status = getApiErrorStatus(error);
          setBlockedIpErrorStatus(isCommonHttpErrorStatus(status) ? status : null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsBlockedIpLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [activeSection, blockedIpPage, blockedIpReloadSequence, blockedIpPageData.items.length]);

  useEffect(() => {
    if (!isTrendPageEditing) {
      setTrendPageDraft(String(trendPageData.currentPage));
    }
  }, [isTrendPageEditing, trendPageData.currentPage]);

  useEffect(() => {
    if (!isBlockedUserPageEditing) {
      setBlockedUserPageDraft(String(blockedUserPageData.currentPage));
    }
  }, [isBlockedUserPageEditing, blockedUserPageData.currentPage]);

  useEffect(() => {
    if (!isBlockedIpPageEditing) {
      setBlockedIpPageDraft(String(blockedIpPageData.currentPage));
    }
  }, [isBlockedIpPageEditing, blockedIpPageData.currentPage]);

  useEffect(() => {
    if (trendPage > trendPageData.totalPages) {
      setTrendPage(trendPageData.totalPages);
    }
  }, [trendPage, trendPageData.totalPages]);

  useEffect(() => {
    if (blockedUserPage > blockedUserPageData.totalPages) {
      setBlockedUserPage(blockedUserPageData.totalPages);
    }
  }, [blockedUserPage, blockedUserPageData.totalPages]);

  useEffect(() => {
    if (blockedIpPage > blockedIpPageData.totalPages) {
      setBlockedIpPage(blockedIpPageData.totalPages);
    }
  }, [blockedIpPage, blockedIpPageData.totalPages]);

  useEffect(() => {
    setTrendPage(1);
    setTrendPageDraft('1');
    setIsTrendPageEditing(false);
  }, [activeRange, customRangeEnd, customRangeStart]);

  const blockedHandles = useMemo(() => new Set(blockedUserPageData.items.map((item) => item.handle)), [blockedUserPageData.items]);

  function applyTrendPageJump() {
    const parsedPage = Number.parseInt(trendPageDraft, 10);
    const nextPage = Number.isNaN(parsedPage) ? trendPageData.currentPage : Math.min(trendPageData.totalPages, Math.max(1, parsedPage));

    setTrendPageDraft(String(nextPage));
    setIsTrendPageEditing(false);
    if (nextPage !== trendPageData.currentPage) {
      setTrendPage(nextPage);
    }
  }

  function applyBlockedUserPageJump() {
    const parsedPage = Number.parseInt(blockedUserPageDraft, 10);
    const nextPage = Number.isNaN(parsedPage) ? blockedUserPageData.currentPage : Math.min(blockedUserPageData.totalPages, Math.max(1, parsedPage));

    setBlockedUserPageDraft(String(nextPage));
    setIsBlockedUserPageEditing(false);
    if (nextPage !== blockedUserPageData.currentPage) {
      setBlockedUserPage(nextPage);
    }
  }

  function applyBlockedIpPageJump() {
    const parsedPage = Number.parseInt(blockedIpPageDraft, 10);
    const nextPage = Number.isNaN(parsedPage) ? blockedIpPageData.currentPage : Math.min(blockedIpPageData.totalPages, Math.max(1, parsedPage));

    setBlockedIpPageDraft(String(nextPage));
    setIsBlockedIpPageEditing(false);
    if (nextPage !== blockedIpPageData.currentPage) {
      setBlockedIpPage(nextPage);
    }
  }

  async function handleBlockUser(handle: string) {
    setActingKey(`block:${handle}`);

    try {
      await blockAdminUser(handle);
      setBlockedUserReloadSequence((value) => value + 1);
      setBlockedIpReloadSequence((value) => value + 1);
    } finally {
      setActingKey((currentKey) => (currentKey === `block:${handle}` ? null : currentKey));
    }
  }

  async function handleUnblockUser(handle: string) {
    setActingKey(`unblock-user:${handle}`);

    try {
      await unblockAdminUser(handle);
      setBlockedUserReloadSequence((value) => value + 1);
      setBlockedIpReloadSequence((value) => value + 1);
    } finally {
      setActingKey((currentKey) => (currentKey === `unblock-user:${handle}` ? null : currentKey));
    }
  }

  async function handleUnblockIp(ipAddress: string) {
    setActingKey(`unblock-ip:${ipAddress}`);

    try {
      await unblockAdminIp(ipAddress);
      setBlockedIpReloadSequence((value) => value + 1);
    } finally {
      setActingKey((currentKey) => (currentKey === `unblock-ip:${ipAddress}` ? null : currentKey));
    }
  }

  function applyCustomRangeDraft() {
    const parsedStart = parseDateTimeInputValue(customRangeStartDraft);
    const parsedEnd = parseDateTimeInputValue(customRangeEndDraft);
    if (!parsedStart || !parsedEnd) {
      setCustomRangeErrorMessage(text('ANOMALY_RANGE_FORMAT_FAIL_MESSAGE', 'YYYY-MM-DD HH24:Mi 형식으로 입력해 주세요.'));
      return;
    }

    if (parsedStart.getTime() > parsedEnd.getTime()) {
      setCustomRangeErrorMessage(text('ANOMALY_RANGE_ORDER_FAIL_MESSAGE', '시작 일시는 종료 일시보다 늦을 수 없습니다.'));
      return;
    }

    setCustomRangeErrorMessage(null);
    setCustomRangeStart(customRangeStartDraft);
    setCustomRangeEnd(customRangeEndDraft);
  }

  function renderTrendBody() {
    if (trendErrorMessage) {
      return trendErrorStatus != null
        ? <HttpErrorState status={trendErrorStatus} className="submit-history-empty-state" message={trendErrorMessage} />
        : <PageLoadFailureState className="submit-history-empty-state" message={trendErrorMessage} />;
    }

    return (
      <>
        <div className="admin-anomaly-range-toolbar">
          <div className="solve-dbms-tab-row admin-anomaly-range-tabs" role="tablist" aria-label={text('ANOMALY_RANGE_TABLIST_LABEL', '이상계정 조회 범위')}>
            {anomalyRanges.map((range) => {
              const isSelected = range.value === activeRange;
              return (
                <button
                  key={range.value}
                  type="button"
                  className={`solve-dbms-tab ${isSelected ? 'is-selected' : ''}`}
                  role="tab"
                  aria-selected={isSelected}
                  onClick={() => setActiveRange(range.value)}
                >
                  {range.label}
                </button>
              );
            })}
          </div>

          {activeRange === 'custom' ? (
            <div className="admin-anomaly-custom-range-fields">
              <input
                type="text"
                className="text-field admin-anomaly-datetime-input"
                value={customRangeStartDraft}
                onChange={(event) => setCustomRangeStartDraft(event.target.value)}
                onBlur={applyCustomRangeDraft}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    applyCustomRangeDraft();
                  }
                }}
                placeholder={text('ANOMALY_RANGE_INPUT_PLACEHOLDER', 'YYYY-MM-DD HH24:Mi')}
                aria-label={text('ANOMALY_RANGE_START_INPUT_LABEL', '조회 시작 일시')}
              />
              <span className="admin-anomaly-custom-range-separator">~</span>
              <input
                type="text"
                className="text-field admin-anomaly-datetime-input"
                value={customRangeEndDraft}
                onChange={(event) => setCustomRangeEndDraft(event.target.value)}
                onBlur={applyCustomRangeDraft}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    applyCustomRangeDraft();
                  }
                }}
                placeholder={text('ANOMALY_RANGE_INPUT_PLACEHOLDER', 'YYYY-MM-DD HH24:Mi')}
                aria-label={text('ANOMALY_RANGE_END_INPUT_LABEL', '조회 종료 일시')}
              />
            </div>
          ) : null}
        </div>

        {activeRange === 'custom' && customRangeErrorMessage ? <p className="admin-anomaly-range-feedback is-error">{customRangeErrorMessage}</p> : null}

        <div className={`submit-history-table-shell ${isTrendLoading ? 'is-loading' : ''}`.trim()}>
          {isTrendLoading && trendPageData.items.length === 0 ? (
            <div className="submit-history-table admin-anomaly-table admin-anomaly-trend-table" role="table" aria-label={text('ANOMALY_TREND_TABLE_LABEL', '이상계정 추이 목록')} aria-hidden="true">
              <div className="submit-history-row submit-history-head" role="row">
                <div role="columnheader" className="submit-history-head-cell">{text('COMMON_HANDLE_LABEL', 'Handle')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('ANOMALY_ACTION_TYPE_COLUMN_LABEL', '실행/제출')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('ANOMALY_COUNT_COLUMN_LABEL', '횟수')}</div>
                <div role="columnheader" className="submit-history-head-cell admin-anomaly-action-head" aria-label={text('ANOMALY_BLOCK_ACTION_LABEL', '차단')} />
              </div>

              {anomalyLoadingRows.map((rowIndex) => (
                <div key={`anomaly-trend-loading-${rowIndex}`} className="submit-history-row submit-history-body" role="row">
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                  <span className="submit-history-cell admin-anomaly-action-cell" role="cell"><span className="wave-loading-placeholder is-mini" /></span>
                </div>
              ))}
            </div>
          ) : trendPageData.items.length === 0 ? (
            <div className="submit-history-empty-state">{text('ANOMALY_EMPTY_STATE', '표시할 데이터가 없습니다.')}</div>
          ) : (
            <div className="submit-history-table admin-anomaly-table admin-anomaly-trend-table" role="table" aria-label={text('ANOMALY_TREND_TABLE_LABEL', '이상계정 추이 목록')}>
              <div className="submit-history-row submit-history-head" role="row">
                <div role="columnheader" className="submit-history-head-cell">{text('COMMON_HANDLE_LABEL', 'Handle')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('ANOMALY_ACTION_TYPE_COLUMN_LABEL', '실행/제출')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('ANOMALY_COUNT_COLUMN_LABEL', '횟수')}</div>
                <div role="columnheader" className="submit-history-head-cell admin-anomaly-action-head" aria-label={text('ANOMALY_BLOCK_ACTION_LABEL', '차단')} />
              </div>

              {trendPageData.items.map((item: AdminAnomalyTrendItem) => {
                const isBlocked = blockedHandles.has(item.handle);
                const isActing = actingKey === `block:${item.handle}`;
                return (
                  <article key={`${item.handle}-${item.actionType}`} className="submit-history-row submit-history-body" role="row">
                    <span className="submit-history-cell" role="cell" data-label={text('COMMON_HANDLE_LABEL', 'Handle')}>{item.handle}</span>
                    <span className="submit-history-cell" role="cell" data-label={text('ANOMALY_ACTION_TYPE_COLUMN_LABEL', '실행/제출')}>{item.actionType}</span>
                    <span className="submit-history-cell" role="cell" data-label={text('ANOMALY_COUNT_COLUMN_LABEL', '횟수')}>{item.count}</span>
                    <span className="submit-history-cell admin-anomaly-action-cell" role="cell" data-label={text('ANOMALY_BLOCK_ACTION_LABEL', '차단')}>
                      <button
                        type="button"
                        className={`submit-history-detail-button admin-anomaly-action-button ${isBlocked ? 'is-blocked' : ''}`.trim()}
                        aria-label={isBlocked ? text('ANOMALY_ALREADY_BLOCKED_LABEL', '이미 차단된 계정') : `${item.handle} ${text('ANOMALY_BLOCK_ACTION_LABEL', '차단')}`}
                        title={isBlocked ? text('ANOMALY_ALREADY_BLOCKED_TITLE', '이미 차단됨') : text('ANOMALY_BLOCK_TITLE', '계정 차단')}
                        onClick={() => {
                          void handleBlockUser(item.handle);
                        }}
                        disabled={isBlocked || isActing}
                      >
                        <BanIcon />
                      </button>
                    </span>
                  </article>
                );
              })}
            </div>
          )}

          {isTrendLoading ? <LoadingOverlay ariaHidden /> : null}
        </div>

        <Pagination
          currentPage={trendPageData.currentPage}
          totalPages={trendPageData.totalPages}
          draft={trendPageDraft}
          isEditing={isTrendPageEditing}
          navigationLabel={text('ANOMALY_TREND_PAGE_LABEL', '이상계정 추이 페이지')}
          inputLabel={text('ANOMALY_TREND_PAGE_INPUT_LABEL', '이상계정 추이 페이지 번호')}
          previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
          nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
          onDraftChange={setTrendPageDraft}
          onStartEditing={() => {
            setTrendPageDraft(String(trendPageData.currentPage));
            setIsTrendPageEditing(true);
          }}
          onCancelEditing={() => {
            setTrendPageDraft(String(trendPageData.currentPage));
            setIsTrendPageEditing(false);
          }}
          onApplyEditing={applyTrendPageJump}
          onPageChange={setTrendPage}
        />
      </>
    );
  }

  function renderBlockedUsersBody() {
    if (blockedUserErrorMessage) {
      return blockedUserErrorStatus != null
        ? <HttpErrorState status={blockedUserErrorStatus} className="submit-history-empty-state" message={blockedUserErrorMessage} />
        : <PageLoadFailureState className="submit-history-empty-state" message={blockedUserErrorMessage} />;
    }

    return (
      <>
        <div className={`submit-history-table-shell ${isBlockedUserLoading ? 'is-loading' : ''}`.trim()}>
          {isBlockedUserLoading && blockedUserPageData.items.length === 0 ? (
            <div className="submit-history-table admin-anomaly-table admin-anomaly-blocked-user-table" role="table" aria-label={text('ANOMALY_BLOCKED_USERS_TABLE_LABEL', '차단 계정 목록')} aria-hidden="true">
              <div className="submit-history-row submit-history-head" role="row">
                <div role="columnheader" className="submit-history-head-cell">{text('COMMON_HANDLE_LABEL', 'Handle')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('COMMON_IP_LABEL', 'IP')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('ANOMALY_BLOCKED_AT_COLUMN_LABEL', '차단 일시')}</div>
                <div role="columnheader" className="submit-history-head-cell admin-anomaly-action-head" aria-label={text('ANOMALY_UNBLOCK_TITLE', '차단 해제')} />
              </div>

              {anomalyLoadingRows.map((rowIndex) => (
                <div key={`anomaly-blocked-user-loading-${rowIndex}`} className="submit-history-row submit-history-body" role="row">
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                  <span className="submit-history-cell admin-anomaly-action-cell" role="cell"><span className="wave-loading-placeholder is-mini" /></span>
                </div>
              ))}
            </div>
          ) : blockedUserPageData.items.length === 0 ? (
            <div className="submit-history-empty-state">{text('ANOMALY_BLOCKED_USERS_EMPTY_STATE', '차단된 계정이 없습니다.')}</div>
          ) : (
            <div className="submit-history-table admin-anomaly-table admin-anomaly-blocked-user-table" role="table" aria-label={text('ANOMALY_BLOCKED_USERS_TABLE_LABEL', '차단 계정 목록')}>
              <div className="submit-history-row submit-history-head" role="row">
                <div role="columnheader" className="submit-history-head-cell">{text('COMMON_HANDLE_LABEL', 'Handle')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('COMMON_IP_LABEL', 'IP')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('ANOMALY_BLOCKED_AT_COLUMN_LABEL', '차단 일시')}</div>
                <div role="columnheader" className="submit-history-head-cell admin-anomaly-action-head" aria-label={text('ANOMALY_UNBLOCK_TITLE', '차단 해제')} />
              </div>

              {blockedUserPageData.items.map((item: AdminBlockedUserItem) => {
                const isActing = actingKey === `unblock-user:${item.handle}`;
                return (
                  <article key={item.handle} className="submit-history-row submit-history-body" role="row">
                    <span className="submit-history-cell" role="cell" data-label={text('COMMON_HANDLE_LABEL', 'Handle')}>{item.handle}</span>
                    <span className="submit-history-cell" role="cell" data-label={text('COMMON_IP_LABEL', 'IP')}>{item.ipAddress || '-'}</span>
                    <span className="submit-history-cell" role="cell" data-label={text('ANOMALY_BLOCKED_AT_COLUMN_LABEL', '차단 일시')}>{formatDateTime(item.blockedAt)}</span>
                    <span className="submit-history-cell admin-anomaly-action-cell" role="cell" data-label={text('ANOMALY_UNBLOCK_TITLE', '차단 해제')}>
                      <button
                        type="button"
                        className="submit-history-detail-button admin-anomaly-action-button"
                        aria-label={`${item.handle} ${text('ANOMALY_UNBLOCK_TITLE', '차단 해제')}`}
                        title={text('ANOMALY_UNBLOCK_TITLE', '차단 해제')}
                        onClick={() => {
                          void handleUnblockUser(item.handle);
                        }}
                        disabled={isActing}
                      >
                        <UnblockIcon />
                      </button>
                    </span>
                  </article>
                );
              })}
            </div>
          )}

          {isBlockedUserLoading ? <LoadingOverlay ariaHidden /> : null}
        </div>

        <Pagination
          currentPage={blockedUserPageData.currentPage}
          totalPages={blockedUserPageData.totalPages}
          draft={blockedUserPageDraft}
          isEditing={isBlockedUserPageEditing}
          navigationLabel={text('ANOMALY_BLOCKED_USERS_PAGE_LABEL', '차단 계정 목록 페이지')}
          inputLabel={text('ANOMALY_BLOCKED_USERS_PAGE_INPUT_LABEL', '차단 계정 목록 페이지 번호')}
          previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
          nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
          onDraftChange={setBlockedUserPageDraft}
          onStartEditing={() => {
            setBlockedUserPageDraft(String(blockedUserPageData.currentPage));
            setIsBlockedUserPageEditing(true);
          }}
          onCancelEditing={() => {
            setBlockedUserPageDraft(String(blockedUserPageData.currentPage));
            setIsBlockedUserPageEditing(false);
          }}
          onApplyEditing={applyBlockedUserPageJump}
          onPageChange={setBlockedUserPage}
        />
      </>
    );
  }

  function renderBlockedIpsBody() {
    if (blockedIpErrorMessage) {
      return blockedIpErrorStatus != null
        ? <HttpErrorState status={blockedIpErrorStatus} className="submit-history-empty-state" message={blockedIpErrorMessage} />
        : <PageLoadFailureState className="submit-history-empty-state" message={blockedIpErrorMessage} />;
    }

    return (
      <>
        <div className={`submit-history-table-shell ${isBlockedIpLoading ? 'is-loading' : ''}`.trim()}>
          {isBlockedIpLoading && blockedIpPageData.items.length === 0 ? (
            <div className="submit-history-table admin-anomaly-table admin-anomaly-blocked-ip-table" role="table" aria-label={text('ANOMALY_BLOCKED_IPS_TABLE_LABEL', '차단 IP 목록')} aria-hidden="true">
              <div className="submit-history-row submit-history-head" role="row">
                <div role="columnheader" className="submit-history-head-cell">{text('COMMON_IP_LABEL', 'IP')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('ANOMALY_BLOCKED_AT_COLUMN_LABEL', '차단 일시')}</div>
                <div role="columnheader" className="submit-history-head-cell admin-anomaly-action-head" aria-label={text('ANOMALY_UNBLOCK_TITLE', '차단 해제')} />
              </div>

              {anomalyLoadingRows.map((rowIndex) => (
                <div key={`anomaly-blocked-ip-loading-${rowIndex}`} className="submit-history-row submit-history-body" role="row">
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                  <span className="submit-history-cell admin-anomaly-action-cell" role="cell"><span className="wave-loading-placeholder is-mini" /></span>
                </div>
              ))}
            </div>
          ) : blockedIpPageData.items.length === 0 ? (
            <div className="submit-history-empty-state">{text('ANOMALY_BLOCKED_IPS_EMPTY_STATE', '차단된 IP가 없습니다.')}</div>
          ) : (
            <div className="submit-history-table admin-anomaly-table admin-anomaly-blocked-ip-table" role="table" aria-label={text('ANOMALY_BLOCKED_IPS_TABLE_LABEL', '차단 IP 목록')}>
              <div className="submit-history-row submit-history-head" role="row">
                <div role="columnheader" className="submit-history-head-cell">{text('COMMON_IP_LABEL', 'IP')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('ANOMALY_BLOCKED_AT_COLUMN_LABEL', '차단 일시')}</div>
                <div role="columnheader" className="submit-history-head-cell admin-anomaly-action-head" aria-label={text('ANOMALY_UNBLOCK_TITLE', '차단 해제')} />
              </div>

              {blockedIpPageData.items.map((item: AdminBlockedIpItem) => {
                const isActing = actingKey === `unblock-ip:${item.ipAddress}`;
                return (
                  <article key={item.ipAddress} className="submit-history-row submit-history-body" role="row">
                    <span className="submit-history-cell" role="cell" data-label={text('COMMON_IP_LABEL', 'IP')}>{item.ipAddress}</span>
                    <span className="submit-history-cell" role="cell" data-label={text('ANOMALY_BLOCKED_AT_COLUMN_LABEL', '차단 일시')}>{formatDateTime(item.blockedAt)}</span>
                    <span className="submit-history-cell admin-anomaly-action-cell" role="cell" data-label={text('ANOMALY_UNBLOCK_TITLE', '차단 해제')}>
                      <button
                        type="button"
                        className="submit-history-detail-button admin-anomaly-action-button"
                        aria-label={`${item.ipAddress} ${text('ANOMALY_UNBLOCK_TITLE', '차단 해제')}`}
                        title={text('ANOMALY_UNBLOCK_TITLE', '차단 해제')}
                        onClick={() => {
                          void handleUnblockIp(item.ipAddress);
                        }}
                        disabled={isActing}
                      >
                        <UnblockIcon />
                      </button>
                    </span>
                  </article>
                );
              })}
            </div>
          )}

          {isBlockedIpLoading ? <LoadingOverlay ariaHidden /> : null}
        </div>

        <Pagination
          currentPage={blockedIpPageData.currentPage}
          totalPages={blockedIpPageData.totalPages}
          draft={blockedIpPageDraft}
          isEditing={isBlockedIpPageEditing}
          navigationLabel={text('ANOMALY_BLOCKED_IPS_PAGE_LABEL', '차단 IP 목록 페이지')}
          inputLabel={text('ANOMALY_BLOCKED_IPS_PAGE_INPUT_LABEL', '차단 IP 목록 페이지 번호')}
          previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
          nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
          onDraftChange={setBlockedIpPageDraft}
          onStartEditing={() => {
            setBlockedIpPageDraft(String(blockedIpPageData.currentPage));
            setIsBlockedIpPageEditing(true);
          }}
          onCancelEditing={() => {
            setBlockedIpPageDraft(String(blockedIpPageData.currentPage));
            setIsBlockedIpPageEditing(false);
          }}
          onApplyEditing={applyBlockedIpPageJump}
          onPageChange={setBlockedIpPage}
        />
      </>
    );
  }

  return (
    <section className="panel-card admin-anomaly-panel">
      <div className="admin-anomaly-layout">
        <aside className="admin-anomaly-side-nav" aria-label={text('ANOMALY_SECTION_NAV_LABEL', '이상계정 감지 섹션')}>
          {anomalySections.map((section) => {
            const isSelected = section.id === activeSection;
            return (
              <button
                key={section.id}
                type="button"
                className={`admin-anomaly-side-nav-item ${isSelected ? 'is-selected' : ''}`}
                onClick={() => setActiveSection(section.id)}
              >
                <strong>{section.label}</strong>
              </button>
            );
          })}
        </aside>

        <div className="admin-anomaly-content">
          {activeSection === 'trend' ? renderTrendBody() : null}
          {activeSection === 'blockedUsers' ? renderBlockedUsersBody() : null}
          {activeSection === 'blockedIps' ? renderBlockedIpsBody() : null}
        </div>
      </div>
    </section>
  );
}
