import { useEffect, useMemo, useRef, useState, useSyncExternalStore, type CSSProperties } from 'react';
import {
  ADMIN_PATH,
  COMMUNITY_PATH,
  DASHBOARD_PATH,
  FAVORITES_PATH,
  GUIDE_PATH,
  PROBLEMS_PATH,
  RANKING_PATH,
  SUBMIT_HISTORY_PATH,
  getProfilePath,
  navigate,
} from '../../lib/navigation';
import { logout as requestLogout } from '../../lib/authApi';
import { fetchAlarms, markAlarmRead, markAllAlarmsRead, type AlarmEntry, type AlarmPageData } from '../../lib/alarmApi';
import { subscribeSessionSocketMessages, type SessionSocketMessage } from '../../lib/sessionSocket';
import logoImage from '../../assets/logo.png';
import { useMockSession } from '../../lib/session';
import { getLoginOverlayDescription, OPEN_LOGIN_OVERLAY_EVENT, type OpenLoginOverlayEventDetail } from '../../lib/authOverlay';
import {
  DEFAULT_NOTIFICATION_TEXT,
  NOTIFICATION_UI_TEXT_KEY,
  refreshCachedUiTexts,
  useUiTextValue,
} from '../../lib/uiText';
import { FavoriteStarIcon } from './FavoriteTabButton';
import { getFavoriteTabsSnapshot, navigateToFavoriteTab, subscribeFavoriteTabs } from '../../lib/favoriteTabs';
import './Header.css';
import HeaderAuthOverlay from './HeaderAuthOverlay';

function subscribe(callback: () => void) {
  window.addEventListener('popstate', callback);
  return () => window.removeEventListener('popstate', callback);
}

function getSnapshot() {
  return window.location.pathname;
}

function formatAlarmTime(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${month}-${day} ${hours}:${minutes}`;
}

function truncateAlarmHoverText(value: string) {
  const normalizedValue = value.trim();
  if (normalizedValue.length <= 15) {
    return normalizedValue;
  }

  return `${normalizedValue.slice(0, 15)}...`;
}

function navigateToAlarmTarget(path: string | undefined, hash?: string) {
  if (!path || path.trim() === '') {
    return;
  }

  navigate(`${path}${hash ?? ''}`);
}

interface AlarmCreatedMessage extends SessionSocketMessage {
  type: 'alarm.created';
  alarm?: AlarmEntry;
  unreadCount?: number;
}

const FAVORITE_PANEL_PAGE_SIZE = 5;
const EMPTY_ALARM_PAGE: AlarmPageData = {
  currentPage: 1,
  pageSize: 5,
  totalCount: 0,
  totalPages: 1,
  unreadCount: 0,
  alarms: [],
};

function isAlarmCreatedMessage(message: SessionSocketMessage): message is AlarmCreatedMessage {
  return message.type === 'alarm.created';
}

function AlarmListIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="M4.5 5.4h11M4.5 10h11M4.5 14.6h11" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.7" />
    </svg>
  );
}

export default function Header() {
  const { isAuthenticated, isReady, isAdmin, isProblemGenerator, logout } = useMockSession();
  const pathname = useSyncExternalStore(subscribe, getSnapshot, () => '/');
  const [isAlarmOpen, setIsAlarmOpen] = useState(false);
  const [isHeaderAuthOverlayOpen, setIsHeaderAuthOverlayOpen] = useState(false);
  const [headerAuthOverlayDescription, setHeaderAuthOverlayDescription] = useState<string | null>(null);
  const [incomingAlarm, setIncomingAlarm] = useState<AlarmEntry | null>(null);
  const [isFavoriteOpen, setIsFavoriteOpen] = useState(false);
  const [alarmPage, setAlarmPage] = useState<AlarmPageData>(EMPTY_ALARM_PAGE);
  const [requestedAlarmPage, setRequestedAlarmPage] = useState(1);
  const [isAlarmLoading, setIsAlarmLoading] = useState(false);
  const [isAlarmPageJumpEditing, setIsAlarmPageJumpEditing] = useState(false);
  const [alarmPageJumpDraft, setAlarmPageJumpDraft] = useState('1');
  const favoriteTabs = useSyncExternalStore(subscribeFavoriteTabs, getFavoriteTabsSnapshot, () => []);
  const [favoritePage, setFavoritePage] = useState(1);
  const [isFavoritePageJumpEditing, setIsFavoritePageJumpEditing] = useState(false);
  const [favoritePageJumpDraft, setFavoritePageJumpDraft] = useState('1');
  const marqueeMessage = useUiTextValue(NOTIFICATION_UI_TEXT_KEY, DEFAULT_NOTIFICATION_TEXT);
  const [marqueeMetrics, setMarqueeMetrics] = useState<{
    startOffset: number;
    endOffset: number;
    durationSeconds: number;
  } | null>(null);
  const alarmRootRef = useRef<HTMLDivElement | null>(null);
  const favoriteRootRef = useRef<HTMLDivElement | null>(null);
  const marqueeShellRef = useRef<HTMLDivElement | null>(null);
  const marqueeCopyRef = useRef<HTMLSpanElement | null>(null);
  const floatingHeaderRef = useRef<HTMLDivElement | null>(null);

  const activeNav = pathname.startsWith(RANKING_PATH)
    ? 'ranking'
    : pathname.startsWith(SUBMIT_HISTORY_PATH)
      ? 'submitHistory'
      : pathname.startsWith(FAVORITES_PATH)
        ? 'favorites'
        : pathname.startsWith(GUIDE_PATH)
          ? 'guide'
          : pathname.startsWith(ADMIN_PATH)
            ? 'admin'
            : pathname.startsWith(COMMUNITY_PATH)
              ? 'community'
              : pathname.startsWith(PROBLEMS_PATH)
                ? 'problems'
                : null;

  const favoriteTotalPages = Math.max(1, Math.ceil(favoriteTabs.length / FAVORITE_PANEL_PAGE_SIZE));
  const favoriteVisibleTabs = useMemo(() => {
    const startIndex = (favoritePage - 1) * FAVORITE_PANEL_PAGE_SIZE;
    return favoriteTabs.slice(startIndex, startIndex + FAVORITE_PANEL_PAGE_SIZE);
  }, [favoritePage, favoriteTabs]);
  const isFloatingHeaderVisible = true;

  useEffect(() => {
    setIsAlarmOpen(false);
    setIsFavoriteOpen(false);
    setIsHeaderAuthOverlayOpen(false);
    setHeaderAuthOverlayDescription(null);
  }, [pathname]);

  useEffect(() => {
    function handleOpenLoginOverlay(event: Event) {
      if (isAuthenticated) {
        return;
      }

      const { description = null } = (event as CustomEvent<OpenLoginOverlayEventDetail>).detail ?? {};
      setHeaderAuthOverlayDescription(description);
      setIsHeaderAuthOverlayOpen(true);
    }

    window.addEventListener(OPEN_LOGIN_OVERLAY_EVENT, handleOpenLoginOverlay);
    return () => window.removeEventListener(OPEN_LOGIN_OVERLAY_EVENT, handleOpenLoginOverlay);
  }, [isAuthenticated]);

  useEffect(() => {
    if (!isReady) {
      return;
    }

    if (!isAuthenticated) {
      setAlarmPage(EMPTY_ALARM_PAGE);
      setRequestedAlarmPage(1);
      setIsAlarmPageJumpEditing(false);
      setAlarmPageJumpDraft('1');
      setIncomingAlarm(null);
      setIsAlarmOpen(false);
      return;
    }

    let cancelled = false;
    setIsAlarmLoading(true);

    fetchAlarms(requestedAlarmPage)
      .then((nextAlarmPage) => {
        if (cancelled) {
          return;
        }

        setAlarmPage(nextAlarmPage);
        if (nextAlarmPage.currentPage !== requestedAlarmPage) {
          setRequestedAlarmPage(nextAlarmPage.currentPage);
        }
      })
      .catch(() => {
        if (cancelled) {
          return;
        }

        setAlarmPage((currentAlarmPage) => ({
          ...currentAlarmPage,
          currentPage: requestedAlarmPage,
        }));
      })
      .finally(() => {
        if (!cancelled) {
          setIsAlarmLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, isReady, requestedAlarmPage]);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }

    return subscribeSessionSocketMessages((message) => {
      if (!isAlarmCreatedMessage(message) || message.alarm == null) {
        return;
      }

      setIncomingAlarm(message.alarm);

      setAlarmPage((currentAlarmPage) => {
        const nextTotalCount = currentAlarmPage.totalCount + 1;
        const nextTotalPages = Math.max(1, Math.ceil(nextTotalCount / currentAlarmPage.pageSize));
        const nextUnreadCount = typeof message.unreadCount === 'number' ? message.unreadCount : currentAlarmPage.unreadCount + 1;

        if (currentAlarmPage.currentPage !== 1) {
          return {
            ...currentAlarmPage,
            totalCount: nextTotalCount,
            totalPages: nextTotalPages,
            unreadCount: nextUnreadCount,
          };
        }

        const nextAlarms = [message.alarm, ...currentAlarmPage.alarms.filter((alarm) => alarm.alarmId !== message.alarm?.alarmId)]
          .slice(0, currentAlarmPage.pageSize);

        return {
          ...currentAlarmPage,
          totalCount: nextTotalCount,
          totalPages: nextTotalPages,
          unreadCount: nextUnreadCount,
          alarms: nextAlarms,
        };
      });
    });
  }, [isAuthenticated]);

  useEffect(() => {
    if (incomingAlarm == null) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      setIncomingAlarm((currentAlarm) => (currentAlarm?.alarmId === incomingAlarm.alarmId ? null : currentAlarm));
    }, 60_000);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [incomingAlarm]);

  useEffect(() => {
    if (!isAlarmOpen) {
      return;
    }

    function handlePointerDown(event: MouseEvent) {
      if (!alarmRootRef.current?.contains(event.target as Node)) {
        setIsAlarmOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsAlarmOpen(false);
      }
    }

    window.addEventListener('mousedown', handlePointerDown);
    window.addEventListener('keydown', handleEscape);

    return () => {
      window.removeEventListener('mousedown', handlePointerDown);
      window.removeEventListener('keydown', handleEscape);
    };
  }, [isAlarmOpen]);

  useEffect(() => {
    if (isAlarmPageJumpEditing) {
      return;
    }

    setAlarmPageJumpDraft(String(alarmPage.currentPage));
  }, [alarmPage.currentPage, isAlarmPageJumpEditing]);

  useEffect(() => {
    setFavoritePage((currentPage) => Math.min(currentPage, favoriteTotalPages));
  }, [favoriteTotalPages]);

  useEffect(() => {
    if (isFavoritePageJumpEditing) {
      return;
    }

    setFavoritePageJumpDraft(String(favoritePage));
  }, [favoritePage, isFavoritePageJumpEditing]);

  useEffect(() => {
    if (!isFavoriteOpen) {
      setIsFavoritePageJumpEditing(false);
      return;
    }

    function handlePointerDown(event: MouseEvent) {
      if (!favoriteRootRef.current?.contains(event.target as Node)) {
        setIsFavoriteOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsFavoriteOpen(false);
      }
    }

    window.addEventListener('mousedown', handlePointerDown);
    window.addEventListener('keydown', handleEscape);

    return () => {
      window.removeEventListener('mousedown', handlePointerDown);
      window.removeEventListener('keydown', handleEscape);
    };
  }, [isFavoriteOpen]);

  useEffect(() => {
    void refreshCachedUiTexts();

    const intervalId = window.setInterval(() => {
      void refreshCachedUiTexts();
    }, 30_000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, []);

  useEffect(() => {
    const marqueeShell = marqueeShellRef.current;
    const marqueeCopy = marqueeCopyRef.current;
    if (!marqueeShell || !marqueeCopy) {
      return;
    }

    let animationFrameId = 0;

    function updateMarqueeMetrics() {
      animationFrameId = window.requestAnimationFrame(() => {
        const currentMarqueeShell = marqueeShellRef.current;
        const currentMarqueeCopy = marqueeCopyRef.current;
        if (!currentMarqueeShell || !currentMarqueeCopy) {
          return;
        }

        const shellWidth = Math.ceil(currentMarqueeShell.getBoundingClientRect().width);
        const copyWidth = Math.ceil(currentMarqueeCopy.scrollWidth);
        if (shellWidth <= 0 || copyWidth <= 0) {
          return;
        }

        const durationSeconds = Math.max(Number(((shellWidth + copyWidth) / 92).toFixed(2)), 12);

        setMarqueeMetrics((currentMetrics) =>
          currentMetrics != null &&
          currentMetrics.startOffset === shellWidth &&
          currentMetrics.endOffset === -copyWidth &&
          currentMetrics.durationSeconds === durationSeconds
            ? currentMetrics
            : {
                startOffset: shellWidth,
                endOffset: -copyWidth,
                durationSeconds,
              },
        );
      });
    }

    updateMarqueeMetrics();

    const resizeObserver =
      typeof ResizeObserver === 'undefined'
        ? null
        : new ResizeObserver(() => {
            window.cancelAnimationFrame(animationFrameId);
            updateMarqueeMetrics();
          });

    resizeObserver?.observe(marqueeShell);
    resizeObserver?.observe(marqueeCopy);

    return () => {
      window.cancelAnimationFrame(animationFrameId);
      resizeObserver?.disconnect();
    };
  }, [marqueeMessage]);

  async function handleMarkAllAlarmsRead() {
    if (alarmPage.unreadCount === 0) {
      return;
    }

    setAlarmPage((currentAlarmPage) => ({
      ...currentAlarmPage,
      unreadCount: 0,
      alarms: currentAlarmPage.alarms.map((alarm) => ({
        ...alarm,
        read: true,
      })),
    }));

    try {
      await markAllAlarmsRead();
    } catch {
      setRequestedAlarmPage((currentPage) => currentPage);
      void fetchAlarms(alarmPage.currentPage)
        .then(setAlarmPage)
        .catch(() => undefined);
    }
  }

  function markAlarmAsRead(alarm: AlarmEntry) {
    setAlarmPage((currentAlarmPage) => ({
      ...currentAlarmPage,
      unreadCount: alarm.read ? currentAlarmPage.unreadCount : Math.max(0, currentAlarmPage.unreadCount - 1),
      alarms: currentAlarmPage.alarms.map((currentAlarm) =>
        currentAlarm.alarmId === alarm.alarmId
          ? {
              ...currentAlarm,
              read: true,
            }
          : currentAlarm,
      ),
    }));

    setIsAlarmOpen(false);
    setIncomingAlarm((currentAlarm) => (currentAlarm?.alarmId === alarm.alarmId ? null : currentAlarm));
    void markAlarmRead(alarm.alarmId);
  }

  function handleAlarmClick(alarm: AlarmEntry) {
    markAlarmAsRead(alarm);
    navigateToAlarmTarget(alarm.targetPath, alarm.targetHash);
  }

  function renderAlarmSentence(alarm: AlarmEntry) {
    const sentence = alarm.sentence.trim();
    if (sentence === '') {
      return alarm.message;
    }

    const parts: Array<string | JSX.Element> = [];
    const tokenPattern = /(\{[^{}]+\}|\([^()]+\))/g;
    let lastIndex = 0;

    for (const tokenMatch of sentence.matchAll(tokenPattern)) {
      const tokenValue = tokenMatch[0];
      const tokenIndex = tokenMatch.index ?? 0;

      if (tokenIndex > lastIndex) {
        parts.push(sentence.slice(lastIndex, tokenIndex));
      }

      const isBraceToken = tokenValue.startsWith('{');
      const tokenKey = tokenValue.slice(1, -1).trim();
      const binding = alarm.bindings[tokenKey];
      const linkText = isBraceToken
        ? (binding?.text && binding.text.trim() !== '' ? binding.text : tokenKey)
        : tokenValue;
      const tooltipText = !isBraceToken && binding?.text ? truncateAlarmHoverText(binding.text) : undefined;
      const tokenPath = binding?.path ?? alarm.targetPath;
      const tokenHash = binding?.hash ?? alarm.targetHash;

      parts.push(
        <button
          key={`${alarm.alarmId}:${tokenKey}:${tokenIndex}`}
          type="button"
          className="header-alarm-token"
          title={tooltipText}
          onClick={(event) => {
            event.stopPropagation();
            markAlarmAsRead(alarm);
            navigateToAlarmTarget(tokenPath, tokenHash);
          }}
        >
          {linkText}
        </button>,
      );

      lastIndex = tokenIndex + tokenValue.length;
    }

    if (lastIndex < sentence.length) {
      parts.push(sentence.slice(lastIndex));
    }

    return parts.length > 0 ? parts : alarm.message;
  }

  function applyAlarmPageJump() {
    const parsedPage = Number.parseInt(alarmPageJumpDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? alarmPage.currentPage
      : Math.min(alarmPage.totalPages, Math.max(1, parsedPage));

    setRequestedAlarmPage(nextPage);
    setAlarmPageJumpDraft(String(nextPage));
    setIsAlarmPageJumpEditing(false);
  }

  function cancelAlarmPageJump() {
    setAlarmPageJumpDraft(String(alarmPage.currentPage));
    setIsAlarmPageJumpEditing(false);
  }

  function applyFavoritePageJump() {
    const parsedPage = Number.parseInt(favoritePageJumpDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? favoritePage
      : Math.min(favoriteTotalPages, Math.max(1, parsedPage));

    setFavoritePage(nextPage);
    setFavoritePageJumpDraft(String(nextPage));
    setIsFavoritePageJumpEditing(false);
  }

  function cancelFavoritePageJump() {
    setFavoritePageJumpDraft(String(favoritePage));
    setIsFavoritePageJumpEditing(false);
  }

  async function handleLogout() {
    try {
      await requestLogout();
    } catch {
    }

    logout();
  }

  const marqueeTrackStyle =
    marqueeMetrics == null
      ? undefined
      : ({
          '--header-marquee-start': `${marqueeMetrics.startOffset}px`,
          '--header-marquee-end': `${marqueeMetrics.endOffset}px`,
          '--header-marquee-duration': `${marqueeMetrics.durationSeconds}s`,
        } as CSSProperties);

  return (
    <header className="header">
      <div ref={marqueeShellRef} className="header-marquee-shell" aria-label="긴급 공지">
        <div className="header-marquee-track" style={marqueeTrackStyle}>
          <span ref={marqueeCopyRef} className="header-marquee-copy">
            {marqueeMessage}
          </span>
        </div>
      </div>

      <div
        ref={floatingHeaderRef}
        className={`header-floating-shell ${isFloatingHeaderVisible ? 'is-visible' : 'is-hidden'}`}
      >
        <div className="header-inner">
          <div className="header-brand-slot">
            <button
              type="button"
              className="brand-button"
              onClick={() => navigate(DASHBOARD_PATH)}
              aria-label="Quertimizer 홈으로 이동"
            >
              <img className="brand-logo" src={logoImage} alt="quertimizer" />
            </button>
          </div>

          <nav className="header-nav" aria-label="주요 메뉴">
            <button
              type="button"
              className={`nav-pill ${activeNav === 'problems' ? 'is-active' : ''}`}
              onClick={() => navigate(PROBLEMS_PATH)}
            >
              문제
            </button>
            <button
              type="button"
              className={`nav-pill ${activeNav === 'submitHistory' ? 'is-active' : ''}`}
              onClick={() => navigate(SUBMIT_HISTORY_PATH)}
            >
              제출 목록
            </button>
            <button
              type="button"
              className={`nav-pill ${activeNav === 'ranking' ? 'is-active' : ''}`}
              onClick={() => navigate(RANKING_PATH)}
            >
              랭킹
            </button>
            <button
              type="button"
              className={`nav-pill ${activeNav === 'community' ? 'is-active' : ''}`}
              onClick={() => navigate(COMMUNITY_PATH)}
            >
              커뮤니티
            </button>
            <button
              type="button"
              className={`nav-pill ${activeNav === 'guide' ? 'is-active' : ''}`}
              onClick={() => navigate(GUIDE_PATH)}
            >
              가이드
            </button>
            {isAuthenticated && (isAdmin || isProblemGenerator) ? (
              <button
                type="button"
                className={`nav-pill ${activeNav === 'admin' ? 'is-active' : ''}`}
                onClick={() => navigate(isAdmin ? ADMIN_PATH : `${ADMIN_PATH}?tab=problemCreate`)}
              >
                {isAdmin ? '관리자' : '문제 관리'}
              </button>
            ) : null}
          </nav>

          <div className={`header-actions ${isAuthenticated ? 'is-authenticated' : 'is-guest'}`}>
            {isAuthenticated ? (
              <>
                <div className="header-notification" ref={alarmRootRef}>
                  <button
                    type="button"
                    className={`header-notification-button ${isAlarmOpen ? 'is-open' : ''}`}
                    onClick={() => {
                      setIsAlarmOpen((currentState) => !currentState);
                      setIncomingAlarm(null);
                    }}
                    aria-label={alarmPage.unreadCount > 0 ? `알람 열기 (읽지 않음 ${alarmPage.unreadCount}개)` : '알람 열기'}
                    aria-haspopup="dialog"
                    aria-expanded={isAlarmOpen}
                  >
                    <svg className="header-notification-icon" viewBox="0 0 24 24" aria-hidden="true">
                      <path
                        d="M12 3.75a4.25 4.25 0 0 0-4.25 4.25v1.14c0 .9-.28 1.77-.8 2.5l-1.27 1.79a1.75 1.75 0 0 0 1.43 2.77h9.78a1.75 1.75 0 0 0 1.43-2.77l-1.27-1.79a4.3 4.3 0 0 1-.8-2.5V8A4.25 4.25 0 0 0 12 3.75Z"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="1.8"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                      <path
                        d="M9.75 18.25a2.25 2.25 0 0 0 4.5 0"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="1.8"
                        strokeLinecap="round"
                      />
                    </svg>
                    {alarmPage.unreadCount > 0 ? <span className="header-notification-badge">{alarmPage.unreadCount}</span> : null}
                  </button>

                  {incomingAlarm != null && !isAlarmOpen ? (
                    <div className={`header-notification-toast ${incomingAlarm.alarmType === 'FROM_ADMIN' ? 'is-admin' : ''}`.trim()}>
                      <div
                        role="button"
                        tabIndex={0}
                        className="header-notification-toast-card"
                        onClick={() => handleAlarmClick(incomingAlarm)}
                        onKeyDown={(event) => {
                          if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            handleAlarmClick(incomingAlarm);
                          }
                        }}
                      >
                        <div className="header-notification-item-header header-notification-item-header-toast">
                          <span className="header-notification-item-time">{formatAlarmTime(incomingAlarm.createdAt)}</span>
                          <p className="header-alarm-sentence">{renderAlarmSentence(incomingAlarm)}</p>
                        </div>
                      </div>
                    </div>
                  ) : null}

                  {isAlarmOpen ? (
                    <div className="header-notification-panel" role="dialog" aria-label="알람">
                      <div className="header-notification-panel-header">
                        <button
                          type="button"
                          className="btn text header-notification-route-button"
                          aria-label="알림 목록으로 이동"
                          title="알림 목록"
                          onClick={() => {
                            setIncomingAlarm(null);
                            setIsAlarmOpen(false);
                            navigate(`${getProfilePath()}?tab=alarms`);
                          }}
                        >
                          <AlarmListIcon />
                        </button>

                        <button
                          type="button"
                          className="btn text header-notification-mark-button"
                          onClick={() => {
                            void handleMarkAllAlarmsRead();
                          }}
                          disabled={alarmPage.unreadCount === 0}
                        >
                          모두 읽음
                        </button>
                      </div>

                      <div className={`header-notification-list ${isAlarmLoading ? 'is-loading' : ''}`.trim()}>
                        {alarmPage.alarms.length > 0 ? (
                          alarmPage.alarms.map((alarm) => (
                            <div
                              key={alarm.alarmId}
                              role="button"
                              tabIndex={0}
                              className={`header-notification-item ${!alarm.read ? 'is-unread' : ''} ${alarm.alarmType === 'FROM_ADMIN' ? 'is-admin' : ''}`.trim()}
                              onClick={() => handleAlarmClick(alarm)}
                              onKeyDown={(event) => {
                                if (event.key === 'Enter' || event.key === ' ') {
                                  event.preventDefault();
                                  handleAlarmClick(alarm);
                                }
                              }}
                            >
                              <div className="header-notification-item-header">
                                <span className="header-notification-item-time">{formatAlarmTime(alarm.createdAt)}</span>
                                <p className="header-alarm-sentence">{renderAlarmSentence(alarm)}</p>
                              </div>
                            </div>
                          ))
                        ) : (
                          <p className="header-notification-empty">알람이 없습니다.</p>
                        )}
                      </div>

                      {alarmPage.totalPages > 1 ? (
                        <div className="problem-pagination header-alarm-pagination" role="navigation" aria-label="알람 페이지">
                          <button
                            type="button"
                            className="mini-toggle problem-page-button"
                            onClick={() => setRequestedAlarmPage((currentPage) => Math.max(1, currentPage - 1))}
                            disabled={alarmPage.currentPage === 1}
                          >
                            이전
                          </button>

                          {isAlarmPageJumpEditing ? (
                            <input
                              type="text"
                              inputMode="numeric"
                              className="problem-pagination-meta-input"
                              aria-label="이동할 알람 페이지 입력"
                              value={alarmPageJumpDraft}
                              onChange={(event) => {
                                const nextValue = event.target.value.replace(/\D+/g, '');
                                setAlarmPageJumpDraft(nextValue);
                              }}
                              onBlur={applyAlarmPageJump}
                              onKeyDown={(event) => {
                                if (event.key === 'Enter') {
                                  event.preventDefault();
                                  applyAlarmPageJump();
                                  return;
                                }

                                if (event.key === 'Escape') {
                                  event.preventDefault();
                                  cancelAlarmPageJump();
                                }
                              }}
                              autoFocus
                            />
                          ) : (
                            <button
                              type="button"
                              className="problem-pagination-meta problem-pagination-meta-button"
                              aria-label="이동할 알람 페이지 입력 열기"
                              onClick={() => {
                                setAlarmPageJumpDraft(String(alarmPage.currentPage));
                                setIsAlarmPageJumpEditing(true);
                              }}
                            >
                              {`${alarmPage.currentPage} / ${alarmPage.totalPages}`}
                            </button>
                          )}

                          <button
                            type="button"
                            className="mini-toggle problem-page-button"
                            onClick={() => setRequestedAlarmPage((currentPage) => Math.min(alarmPage.totalPages, currentPage + 1))}
                            disabled={alarmPage.currentPage >= alarmPage.totalPages}
                          >
                            다음
                          </button>
                        </div>
                      ) : null}
                    </div>
                  ) : null}
                </div>

                <div className="header-favorite" ref={favoriteRootRef}>
                  <button
                    type="button"
                    className={`header-link-button favorite-link-button ${(activeNav === 'favorites' || isFavoriteOpen) ? 'is-active' : ''}`.trim()}
                    onClick={() => setIsFavoriteOpen((currentState) => !currentState)}
                    aria-label="즐겨찾기"
                    aria-haspopup="dialog"
                    aria-expanded={isFavoriteOpen}
                  >
                    <FavoriteStarIcon filled={activeNav === 'favorites' || isFavoriteOpen} className="favorite-link-button-icon" />
                  </button>

                  {isFavoriteOpen ? (
                    <div className="header-favorite-panel" role="dialog" aria-label="즐겨찾기">
                      {favoriteTabs.length > 0 ? (
                        <>
                          <div className="header-favorite-list">
                            {favoriteVisibleTabs.map((favoriteTab) => (
                              <button
                                key={favoriteTab.path}
                                type="button"
                                className="header-favorite-item"
                                onClick={() => {
                                  setIsFavoriteOpen(false);
                                  navigateToFavoriteTab(favoriteTab);
                                }}
                              >
                                <FavoriteStarIcon filled={true} className="header-favorite-item-icon" />
                                <span className="header-favorite-item-label">{favoriteTab.label}</span>
                              </button>
                            ))}
                          </div>

                          <div className="problem-pagination header-favorite-pagination" role="navigation" aria-label="즐겨찾기 페이지">
                            <button
                              type="button"
                              className="mini-toggle problem-page-button"
                              onClick={() => setFavoritePage((currentPage) => Math.max(1, currentPage - 1))}
                              disabled={favoritePage === 1}
                            >
                              이전
                            </button>

                            {isFavoritePageJumpEditing ? (
                              <input
                                type="text"
                                inputMode="numeric"
                                className="problem-pagination-meta-input"
                                aria-label="이동할 즐겨찾기 페이지 입력"
                                value={favoritePageJumpDraft}
                                onChange={(event) => {
                                  const nextValue = event.target.value.replace(/\D+/g, '');
                                  setFavoritePageJumpDraft(nextValue);
                                }}
                                onBlur={applyFavoritePageJump}
                                onKeyDown={(event) => {
                                  if (event.key === 'Enter') {
                                    event.preventDefault();
                                    applyFavoritePageJump();
                                    return;
                                  }

                                  if (event.key === 'Escape') {
                                    event.preventDefault();
                                    cancelFavoritePageJump();
                                  }
                                }}
                                autoFocus
                              />
                            ) : (
                              <button
                                type="button"
                                className="problem-pagination-meta problem-pagination-meta-button"
                                aria-label="이동할 즐겨찾기 페이지 입력 열기"
                                onClick={() => {
                                  setFavoritePageJumpDraft(String(favoritePage));
                                  setIsFavoritePageJumpEditing(true);
                                }}
                              >
                                {`${favoritePage} / ${favoriteTotalPages}`}
                              </button>
                            )}

                            <button
                              type="button"
                              className="mini-toggle problem-page-button"
                              onClick={() => setFavoritePage((currentPage) => Math.min(favoriteTotalPages, currentPage + 1))}
                              disabled={favoritePage >= favoriteTotalPages}
                            >
                              다음
                            </button>
                          </div>
                        </>
                      ) : (
                        <p className="header-favorite-empty">즐겨찾기 된 페이지가 없습니다.</p>
                      )}
                    </div>
                  ) : null}
                </div>

                <button
                  type="button"
                  className="header-link-button profile-link-button"
                  onClick={() => navigate(getProfilePath())}
                >
                  프로필
                </button>
                <button
                  type="button"
                  className="header-link-button"
                  onClick={() => {
                    void handleLogout();
                  }}
                >
                  로그아웃
                </button>
              </>
            ) : (
              <button
                type="button"
                className="header-link-button"
                onClick={() => {
                  setHeaderAuthOverlayDescription(getLoginOverlayDescription());
                  setIsHeaderAuthOverlayOpen(true);
                }}
              >
                로그인
              </button>
            )}
          </div>
        </div>
      </div>
      {(!isAuthenticated && isHeaderAuthOverlayOpen) ? (
        <HeaderAuthOverlay
          description={headerAuthOverlayDescription}
          onClose={() => setIsHeaderAuthOverlayOpen(false)}
          onAuthenticated={() => setIsHeaderAuthOverlayOpen(false)}
        />
      ) : null}
    </header>
  );
}
