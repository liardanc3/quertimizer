import { useEffect, useMemo, useRef, useState, useSyncExternalStore, type MouseEvent, type ReactElement } from 'react';
import {
  ADMIN_PATH,
  COMMUNITY_PATH,
  DASHBOARD_PATH,
  FAVORITES_PATH,
  GUIDE_PATH,
  PROFILE_ACTIVITY_PATH,
  PROFILE_PATH,
  PROBLEMS_PATH,
  RANKING_PATH,
  SUBMIT_HISTORY_PATH,
  getProfileActivityPath,
  getProfilePath,
  navigate,
} from '@/shared/config/navigation';
import { logout as requestLogout } from '@/shared/api/auth-api';
import { fetchAlarms, markAlarmRead, markAllAlarmsRead, type AlarmEntry, type AlarmPageData } from '@/shared/api/alarm-api';
import { subscribeSessionSocketMessages, type SessionSocketMessage } from '@/shared/auth/session-socket';
import logoImage from '@/shared/assets/logo.png';
import { prepareLogoutReload, useSession } from '@/shared/auth/session';
import { getLoginOverlayDescription, OPEN_LOGIN_OVERLAY_EVENT, type OpenLoginOverlayEventDetail } from '@/shared/auth/auth-overlay';
import {
  TITLE_UI_TEXT_KEY,
  useUiText,
} from '@/shared/config/ui-text';
import { FavoriteStarIcon } from '@/features/favorite-tab';
import { getFavoriteTabsSnapshot, navigateToFavoriteTab, subscribeFavoriteTabs } from '@/features/favorite-tab';
import './Header.css';
import HeaderAuthOverlay from './HeaderAuthOverlay';
import { useLocationPathname } from '@/shared/lib/hooks/use-location-state';
import useDismissableLayer from '@/shared/lib/hooks/use-dismissable-layer';
import { Pagination } from '@/shared/ui';
import { formatAlarmTime } from '@/shared/lib/formatters';
import { AlarmListIcon, BellIcon, MenuIcon } from '@/shared/ui/icons';
import HeaderMarquee from './HeaderMarquee';
import HeaderNav, { type HeaderNavItem } from './HeaderNav';

function truncateAlarmHoverText(value: string) {
  const normalizedValue = value.trim();
  if (normalizedValue.length <= 15) {
    return normalizedValue;
  }

  return `${normalizedValue.slice(0, 15)}…`;
}

function navigateToAlarmTarget(path: string | undefined, hash?: string) {
  if (!path || path.trim() === '') {
    return;
  }

  navigate(`${path}${hash ?? ''}`);
}

type MobileNavIconType = 'problems' | 'submitHistory' | 'ranking' | 'community' | 'guide' | 'admin' | 'profile' | 'logout' | 'login';

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

function resolveLogoutRedirectPath(pathname: string, search: string, currentHandle: string | null) {
  if (currentHandle == null) {
    return `${pathname}${search}`;
  }

  if (pathname === PROFILE_PATH) {
    return getProfilePath(currentHandle);
  }

  if (pathname === PROFILE_ACTIVITY_PATH) {
    return `${getProfileActivityPath(currentHandle)}${search}`;
  }

  return `${pathname}${search}`;
}

function resolveMobileNavIconType(key: string): MobileNavIconType {
  if (key === 'submitHistory' || key === 'ranking' || key === 'community' || key === 'guide' || key === 'admin') {
    return key;
  }

  return 'problems';
}

function MobileNavIcon({ type }: { type: MobileNavIconType }) {
  if (type === 'submitHistory') {
    return (
      <svg viewBox="0 0 18 18" aria-hidden="true">
        <path d="M5.2 4.2h7.6M5.2 7.2h7.6M5.2 10.2h4.2M3.4 2.6h11.2v12.8H3.4V2.6Z" />
      </svg>
    );
  }

  if (type === 'ranking') {
    return (
      <svg viewBox="0 0 18 18" aria-hidden="true">
        <path d="M4.2 13.8V8.7M9 13.8V4.2M13.8 13.8v-7M3.2 13.8h11.6" />
      </svg>
    );
  }

  if (type === 'community') {
    return (
      <svg viewBox="0 0 18 18" aria-hidden="true">
        <path d="M4 4.3h10v6.8H8.2l-3.3 2.5v-2.5H4V4.3Z" />
      </svg>
    );
  }

  if (type === 'guide') {
    return (
      <svg viewBox="0 0 18 18" aria-hidden="true">
        <path d="M4 3.3h6.6a2.4 2.4 0 0 1 2.4 2.4v9H6.4A2.4 2.4 0 0 1 4 12.3v-9ZM7 6.2h3.7M7 9h3.1" />
      </svg>
    );
  }

  if (type === 'admin') {
    return (
      <svg viewBox="0 0 18 18" aria-hidden="true">
        <path d="M9 2.8 14 5v3.8c0 3.1-1.9 5.4-5 6.4-3.1-1-5-3.3-5-6.4V5l5-2.2ZM7.2 8.8 8.6 10.2l2.7-3" />
      </svg>
    );
  }

  if (type === 'profile') {
    return (
      <svg viewBox="0 0 18 18" aria-hidden="true">
        <path d="M9 9.2a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM4.1 14.8c.8-2.3 2.5-3.5 4.9-3.5s4.1 1.2 4.9 3.5" />
      </svg>
    );
  }

  if (type === 'logout') {
    return (
      <svg viewBox="0 0 18 18" aria-hidden="true">
        <path d="M7.5 4.1H4.2v9.8h3.3M10.3 5.8 13.5 9l-3.2 3.2M6.8 9h6.4" />
      </svg>
    );
  }

  if (type === 'login') {
    return (
      <svg viewBox="0 0 18 18" aria-hidden="true">
        <path d="M10.5 4.1h3.3v9.8h-3.3M7.7 5.8 4.5 9l3.2 3.2M4.8 9h6.4" />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 18 18" aria-hidden="true">
      <path d="M4.2 4.6h9.6M4.2 9h9.6M4.2 13.4h6.2" />
    </svg>
  );
}

export default function Header() {
  const { isAuthenticated, isReady, isAdmin, handle: currentHandle } = useSession();
  const { text } = useUiText();
  const pathname = useLocationPathname();
  const [isAlarmOpen, setIsAlarmOpen] = useState(false);
  const [isHeaderAuthOverlayOpen, setIsHeaderAuthOverlayOpen] = useState(false);
  const [headerAuthOverlayDescription, setHeaderAuthOverlayDescription] = useState<string | null>(null);
  const [incomingAlarm, setIncomingAlarm] = useState<AlarmEntry | null>(null);
  const [isFavoriteOpen, setIsFavoriteOpen] = useState(false);
  const [alarmPage, setAlarmPage] = useState<AlarmPageData>(EMPTY_ALARM_PAGE);
  const [requestedAlarmPage, setRequestedAlarmPage] = useState(1);
  const [isAlarmLoading, setIsAlarmLoading] = useState(false);
  const favoriteTabs = useSyncExternalStore(subscribeFavoriteTabs, getFavoriteTabsSnapshot, () => []);
  const [favoritePage, setFavoritePage] = useState(1);
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
  const alarmRootRef = useRef<HTMLDivElement | null>(null);
  const favoriteRootRef = useRef<HTMLDivElement | null>(null);
  const floatingHeaderRef = useRef<HTMLDivElement | null>(null);
  const mobileNavButtonRef = useRef<HTMLButtonElement | null>(null);
  const mobileNavPanelRef = useRef<HTMLDivElement | null>(null);
  const alarmLayerRefs = useMemo(() => [alarmRootRef], []);
  const favoriteLayerRefs = useMemo(() => [favoriteRootRef], []);
  const mobileNavLayerRefs = useMemo(() => [mobileNavButtonRef, mobileNavPanelRef], []);

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
  const headerNavItems = useMemo<HeaderNavItem[]>(
    () => [
      { key: 'problems', label: text('HEADER_MENU_PROBLEMS', '문제'), path: PROBLEMS_PATH, isActive: activeNav === 'problems' },
      { key: 'submitHistory', label: text('HEADER_MENU_SUBMISSIONS', '제출 목록'), path: SUBMIT_HISTORY_PATH, isActive: activeNav === 'submitHistory' },
      { key: 'ranking', label: text('HEADER_MENU_RANKING', '랭킹'), path: RANKING_PATH, isActive: activeNav === 'ranking' },
      { key: 'community', label: text('HEADER_MENU_COMMUNITY', '커뮤니티'), path: COMMUNITY_PATH, isActive: activeNav === 'community' },
      { key: 'guide', label: text('HEADER_MENU_GUIDE', '가이드'), path: GUIDE_PATH, isActive: activeNav === 'guide' },
    ],
    [activeNav, text]
  );

  const favoriteTotalPages = Math.max(1, Math.ceil(favoriteTabs.length / FAVORITE_PANEL_PAGE_SIZE));
  const favoriteVisibleTabs = useMemo(() => {
    const startIndex = (favoritePage - 1) * FAVORITE_PANEL_PAGE_SIZE;
    return favoriteTabs.slice(startIndex, startIndex + FAVORITE_PANEL_PAGE_SIZE);
  }, [favoritePage, favoriteTabs]);
  const isFloatingHeaderVisible = true;
  const visibleHeaderNavItems = useMemo<HeaderNavItem[]>(() => {
    if (!isAuthenticated || !isAdmin) {
      return headerNavItems;
    }

    return [
      ...headerNavItems,
      {
        key: 'admin',
        label: text('HEADER_MENU_ADMIN', '관리자'),
        path: ADMIN_PATH,
        isActive: activeNav === 'admin',
      },
    ];
  }, [activeNav, headerNavItems, isAdmin, isAuthenticated, text]);
  const ownProfilePath = currentHandle != null ? getProfilePath(currentHandle) : null;
  const ownProfileAlarmPath = ownProfilePath != null ? `${ownProfilePath}?tab=alarms` : null;

  useEffect(() => {
    setIsAlarmOpen(false);
    setIsFavoriteOpen(false);
    setIsHeaderAuthOverlayOpen(false);
    setHeaderAuthOverlayDescription(null);
    setIsMobileNavOpen(false);
  }, [pathname]);

  useEffect(() => {
    function handleOpenLoginOverlay(event: Event) {
      const { description = null, force = false } = (event as CustomEvent<OpenLoginOverlayEventDetail>).detail ?? {};
      if (isAuthenticated && !force) {
        return;
      }

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
      const alarm = message.alarm;

      setIncomingAlarm(alarm);

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

        const nextAlarms = [alarm, ...currentAlarmPage.alarms.filter((currentAlarm) => currentAlarm.alarmId !== alarm.alarmId)]
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

  useDismissableLayer({
    enabled: isAlarmOpen,
    refs: alarmLayerRefs,
    onDismiss: () => setIsAlarmOpen(false),
  });

  useEffect(() => {
    setFavoritePage((currentPage) => Math.min(currentPage, favoriteTotalPages));
  }, [favoriteTotalPages]);

  useDismissableLayer({
    enabled: isFavoriteOpen,
    refs: favoriteLayerRefs,
    onDismiss: () => setIsFavoriteOpen(false),
  });

  useEffect(() => {
    if (!isMobileNavOpen) {
      return;
    }

    function handleResize() {
      if (window.innerWidth >= 640) {
        setIsMobileNavOpen(false);
      }
    }

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [isMobileNavOpen]);

  useDismissableLayer({
    enabled: isMobileNavOpen,
    refs: mobileNavLayerRefs,
    onDismiss: () => setIsMobileNavOpen(false),
    dismissOnResize: false,
  });

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

    const parts: Array<string | ReactElement> = [];
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

  function closeMobileNav() {
    setIsMobileNavOpen(false);
  }

  function handleMobileNavigate(path: string) {
    closeMobileNav();
    navigate(path);
  }

  function handleMobileLinkClick(event: MouseEvent<HTMLAnchorElement>, path: string) {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.altKey || event.ctrlKey || event.shiftKey) {
      return;
    }

    event.preventDefault();
    handleMobileNavigate(path);
  }

  async function handleLogout() {
    const nextPath = resolveLogoutRedirectPath(window.location.pathname, window.location.search, currentHandle);

    try {
      await requestLogout();
    } catch {
    }

    prepareLogoutReload();

    if (`${window.location.pathname}${window.location.search}` === nextPath) {
      window.location.reload();
      return;
    }

    window.location.replace(nextPath);
  }

  return (
    <header className="header">
      <HeaderMarquee />

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
              aria-label={text('HEADER_HOME_LINK_LABEL', 'Quertimizer 홈으로 이동')}
            >
              <img className="brand-logo" src={logoImage} alt={text(TITLE_UI_TEXT_KEY, 'Quertimizer')} />
            </button>
          </div>

          <HeaderNav items={visibleHeaderNavItems} label={text('HEADER_NAV_LABEL', '주요 메뉴')} />

          <div className={`header-actions ${isAuthenticated ? 'is-authenticated' : 'is-guest'}`}>
            {isAuthenticated ? (
              <>
                <div className="header-notification" ref={alarmRootRef}>
                  <button
                    type="button"
                    className={`header-notification-button ${isAlarmOpen ? 'is-open' : ''}`}
                    onClick={() => {
                      setIsMobileNavOpen(false);
                      setIsAlarmOpen((currentState) => !currentState);
                      setIncomingAlarm(null);
                    }}
                    aria-label={
                      alarmPage.unreadCount > 0
                        ? text('HEADER_ALARM_OPEN_WITH_UNREAD_LABEL', { count: alarmPage.unreadCount }, '알림 열기 (읽지 않음 {count}개)')
                        : text('HEADER_ALARM_OPEN_LABEL', '알림 열기')
                    }
                    aria-haspopup="dialog"
                    aria-expanded={isAlarmOpen}
                  >
                    <BellIcon />
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
                    <div className="header-notification-panel" role="dialog" aria-label={text('HEADER_ALARM_DIALOG_LABEL', '알림')}>
                      <div className="header-notification-panel-header">
                        <button
                          type="button"
                          className="btn text header-notification-route-button"
                          aria-label={text('HEADER_ALARM_LIST_MOVE_LABEL', '알림 목록으로 이동')}
                          title={text('HEADER_ALARM_LIST_TITLE', '알림 목록')}
                          onClick={() => {
                            setIncomingAlarm(null);
                            setIsAlarmOpen(false);
                            if (ownProfileAlarmPath != null) {
                              navigate(ownProfileAlarmPath);
                            }
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
                          {text('HEADER_ALARM_MARK_ALL_READ_BUTTON', '모두 읽음')}
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
                          <p className="header-notification-empty">{text('HEADER_ALARM_EMPTY_STATE', '알림이 없습니다.')}</p>
                        )}
                      </div>

                      {alarmPage.totalPages > 1 ? (
                        <Pagination
                          className="problem-pagination header-alarm-pagination"
                          currentPage={alarmPage.currentPage}
                          totalPages={alarmPage.totalPages}
                          onPageChange={setRequestedAlarmPage}
                          ariaLabel={text('HEADER_ALARM_PAGE_LABEL', '알림 페이지')}
                          inputLabel={text('HEADER_ALARM_PAGE_INPUT_LABEL', '이동할 알림 페이지 입력')}
                          inputOpenLabel={text('HEADER_ALARM_PAGE_INPUT_OPEN_LABEL', '이동할 알림 페이지 입력 열기')}
                          previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
                          nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
                        />
                      ) : null}
                    </div>
                  ) : null}
                </div>

                <div className="header-favorite" ref={favoriteRootRef}>
                  <button
                    type="button"
                    className={`header-link-button favorite-link-button ${(activeNav === 'favorites' || isFavoriteOpen) ? 'is-active' : ''}`.trim()}
                    onClick={() => {
                      setIsMobileNavOpen(false);
                      setIsFavoriteOpen((currentState) => !currentState);
                    }}
                    aria-label={text('HEADER_FAVORITES_BUTTON_LABEL', '즐겨찾기')}
                    aria-haspopup="dialog"
                    aria-expanded={isFavoriteOpen}
                  >
                    <FavoriteStarIcon filled={activeNav === 'favorites' || isFavoriteOpen} className="favorite-link-button-icon" />
                  </button>

                  {isFavoriteOpen ? (
                    <div className="header-favorite-panel" role="dialog" aria-label={text('HEADER_FAVORITES_DIALOG_LABEL', '즐겨찾기')}>
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

                          <Pagination
                            className="problem-pagination header-favorite-pagination"
                            currentPage={favoritePage}
                            totalPages={favoriteTotalPages}
                            onPageChange={setFavoritePage}
                            ariaLabel={text('HEADER_FAVORITES_PAGE_LABEL', '즐겨찾기 페이지')}
                            inputLabel={text('HEADER_FAVORITES_PAGE_INPUT_LABEL', '이동할 즐겨찾기 페이지 입력')}
                            inputOpenLabel={text('HEADER_FAVORITES_PAGE_INPUT_OPEN_LABEL', '이동할 즐겨찾기 페이지 입력 열기')}
                            previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
                            nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
                          />
                        </>
                      ) : (
                        <p className="header-favorite-empty">{text('HEADER_FAVORITES_EMPTY_STATE', '즐겨찾기한 페이지가 없습니다.')}</p>
                      )}
                    </div>
                  ) : null}
                </div>

                {ownProfilePath != null ? (
                  <button
                    type="button"
                    className="header-link-button profile-link-button"
                    onClick={() => {
                      closeMobileNav();
                      navigate(ownProfilePath);
                    }}
                  >
                    {text('HEADER_PROFILE_BUTTON', '프로필')}
                  </button>
                ) : null}
                <button
                  type="button"
                  className="header-link-button is-logout"
                  onClick={() => {
                    closeMobileNav();
                    void handleLogout();
                  }}
                >
                  {text('HEADER_LOGOUT_BUTTON', '로그아웃')}
                </button>
              </>
            ) : (
              <button
                type="button"
                className="header-link-button"
                onClick={() => {
                  closeMobileNav();
                  setHeaderAuthOverlayDescription(getLoginOverlayDescription());
                  setIsHeaderAuthOverlayOpen(true);
                }}
              >
                {text('HEADER_LOGIN_BUTTON', '로그인')}
              </button>
            )}

            <button
              ref={mobileNavButtonRef}
              type="button"
              className={`header-mobile-menu-button ${isMobileNavOpen ? 'is-open' : ''}`.trim()}
              aria-label={isMobileNavOpen ? text('HEADER_MOBILE_MENU_CLOSE_LABEL', '모바일 메뉴 닫기') : text('HEADER_MOBILE_MENU_OPEN_LABEL', '모바일 메뉴 열기')}
              aria-haspopup="dialog"
              aria-expanded={isMobileNavOpen}
              aria-controls="header-mobile-nav-panel"
              onClick={() => {
                setIsAlarmOpen(false);
                setIsFavoriteOpen(false);
                setIsMobileNavOpen((currentState) => !currentState);
              }}
            >
              <MenuIcon open={isMobileNavOpen} />
            </button>
          </div>
        </div>
      </div>

      {isMobileNavOpen ? (
        <div className="header-mobile-nav-layer">
          <div
            ref={mobileNavPanelRef}
            id="header-mobile-nav-panel"
            className="header-mobile-nav-panel"
            role="dialog"
            aria-modal="true"
            aria-label={text('HEADER_MOBILE_MENU_DIALOG_LABEL', '모바일 메뉴')}
          >
            <div className="header-mobile-nav-group">
              {visibleHeaderNavItems.map((item) => (
                <a
                  key={`mobile-${item.key}`}
                  href={item.path}
                  className={`header-mobile-nav-item ${item.isActive ? 'is-active' : ''}`.trim()}
                  aria-current={item.isActive ? 'page' : undefined}
                  onClick={(event) => handleMobileLinkClick(event, item.path)}
                >
                  <span className="header-mobile-nav-item-icon"><MobileNavIcon type={resolveMobileNavIconType(item.key)} /></span>
                  <span className="header-mobile-nav-item-label">{item.label}</span>
                </a>
              ))}
            </div>

            <div className="header-mobile-nav-group is-secondary">
              {isAuthenticated ? (
                <>
                  {ownProfilePath != null ? (
                    <button
                      type="button"
                      className="header-mobile-nav-item"
                      onClick={() => handleMobileNavigate(ownProfilePath)}
                    >
                      <span className="header-mobile-nav-item-icon"><MobileNavIcon type="profile" /></span>
                      <span className="header-mobile-nav-item-label">{text('HEADER_PROFILE_BUTTON', '프로필')}</span>
                    </button>
                  ) : null}
                  <button
                    type="button"
                    className="header-mobile-nav-item is-danger"
                    onClick={() => {
                      closeMobileNav();
                      void handleLogout();
                    }}
                  >
                    <span className="header-mobile-nav-item-icon"><MobileNavIcon type="logout" /></span>
                    <span className="header-mobile-nav-item-label">{text('HEADER_LOGOUT_BUTTON', '로그아웃')}</span>
                  </button>
                </>
              ) : (
                <button
                  type="button"
                  className="header-mobile-nav-primary"
                  onClick={() => {
                    closeMobileNav();
                    setHeaderAuthOverlayDescription(getLoginOverlayDescription());
                    setIsHeaderAuthOverlayOpen(true);
                  }}
                >
                  <span className="header-mobile-nav-item-icon"><MobileNavIcon type="login" /></span>
                  <span className="header-mobile-nav-item-label">{text('HEADER_LOGIN_BUTTON', '로그인')}</span>
                </button>
              )}
            </div>
          </div>
        </div>
      ) : null}
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
