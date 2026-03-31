import { useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import {
  COMMUNITY_PATH,
  LANDING_SIGNUP_PATH,
  PROBLEMS_PATH,
  RANKING_PATH,
  getProfilePath,
  navigate,
} from '../../lib/navigation';
import { useMockSession } from '../../lib/session';
import { mockNotifications } from '../../mocks/notifications';

function subscribe(callback: () => void) {
  window.addEventListener('popstate', callback);
  return () => window.removeEventListener('popstate', callback);
}

function getSnapshot() {
  return window.location.pathname;
}

function focusAuthForm() {
  if (window.location.pathname !== '/' || window.location.hash) {
    navigate('/', { replace: true });
  }

  window.requestAnimationFrame(() => {
    window.requestAnimationFrame(() => {
      const form = document.getElementById('auth-form');
      form?.scrollIntoView({ behavior: 'smooth', block: 'center' });

      const firstInput = form?.querySelector('input');
      if (firstInput instanceof HTMLInputElement) {
        firstInput.focus();
      }
    });
  });
}

function formatNotificationTime(value: string) {
  const date = new Date(value);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${month}-${day} ${hours}:${minutes}`;
}

export default function Header() {
  const { isAuthenticated, logout } = useMockSession();
  const pathname = useSyncExternalStore(subscribe, getSnapshot, () => '/');
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [notifications, setNotifications] = useState(mockNotifications);
  const notificationRootRef = useRef<HTMLDivElement | null>(null);

  const activeNav = pathname.startsWith(RANKING_PATH)
    ? 'ranking'
    : pathname.startsWith(COMMUNITY_PATH)
      ? 'community'
      : pathname.startsWith(PROBLEMS_PATH)
        ? 'problems'
        : null;

  const unreadCount = useMemo(
    () => notifications.filter((notification) => notification.isUnread).length,
    [notifications]
  );

  useEffect(() => {
    setIsNotificationOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!isNotificationOpen) {
      return;
    }

    function handlePointerDown(event: MouseEvent) {
      if (!notificationRootRef.current?.contains(event.target as Node)) {
        setIsNotificationOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsNotificationOpen(false);
      }
    }

    window.addEventListener('mousedown', handlePointerDown);
    window.addEventListener('keydown', handleEscape);

    return () => {
      window.removeEventListener('mousedown', handlePointerDown);
      window.removeEventListener('keydown', handleEscape);
    };
  }, [isNotificationOpen]);

  function handleMarkAllRead() {
    setNotifications((currentNotifications) =>
      currentNotifications.map((notification) => ({
        ...notification,
        isUnread: false,
      }))
    );
  }

  function handleNotificationClick(notificationId: string, href: string) {
    setNotifications((currentNotifications) =>
      currentNotifications.map((notification) =>
        notification.id === notificationId
          ? {
              ...notification,
              isUnread: false,
            }
          : notification
      )
    );

    setIsNotificationOpen(false);
    navigate(href);
  }

  return (
    <header className="header">
      <div className="header-inner">
        <div className="header-brand-slot">
          <button
            type="button"
            className="brand-button"
            onClick={() => navigate(isAuthenticated ? PROBLEMS_PATH : '/')}
            aria-label="quertimizer 홈으로 이동"
          >
            <img className="brand-logo" src="/favicon.svg" alt="quertimizer" />
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
        </nav>

        <div className={`header-actions ${isAuthenticated ? 'is-authenticated' : 'is-guest'}`}>
          {isAuthenticated ? (
            <>
              <div className="header-notification" ref={notificationRootRef}>
                <button
                  type="button"
                  className={`header-notification-button ${isNotificationOpen ? 'is-open' : ''}`}
                  onClick={() => setIsNotificationOpen((currentState) => !currentState)}
                  aria-label={unreadCount > 0 ? `알림 열기 (읽지 않음 ${unreadCount}개)` : '알림 열기'}
                  aria-haspopup="dialog"
                  aria-expanded={isNotificationOpen}
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
                  {unreadCount > 0 ? <span className="header-notification-badge">{unreadCount}</span> : null}
                </button>

                {isNotificationOpen ? (
                  <div className="header-notification-panel" role="dialog" aria-label="알림">
                    <div className="header-notification-panel-header">
                      <div className="header-notification-panel-copy">
                        <p className="panel-meta">알림</p>
                        <h2 className="header-notification-panel-title">알림함</h2>
                      </div>
                      <button
                        type="button"
                        className="btn text header-notification-mark-button"
                        onClick={handleMarkAllRead}
                        disabled={unreadCount === 0}
                      >
                        모두 읽음
                      </button>
                    </div>

                    <div className="header-notification-panel-status">
                      <span className="subtle-chip">읽지 않음 {unreadCount}개</span>
                    </div>

                    <div className="header-notification-list">
                      {notifications.map((notification) => (
                        <button
                          key={notification.id}
                          type="button"
                          className={`header-notification-item ${notification.isUnread ? 'is-unread' : ''}`}
                          onClick={() => handleNotificationClick(notification.id, notification.href)}
                        >
                          <div className="header-notification-item-header">
                            <strong>{notification.title}</strong>
                            <span>{formatNotificationTime(notification.createdAt)}</span>
                          </div>
                          <p>{notification.message}</p>
                        </button>
                      ))}
                    </div>
                  </div>
                ) : null}
              </div>

              <button type="button" className="header-link-button profile-link-button" onClick={() => navigate(getProfilePath())}>
                프로필
              </button>
              <button
                type="button"
                className="header-link-button"
                onClick={() => {
                  logout();
                  navigate('/', { replace: true });
                }}
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <button type="button" className="header-link-button" onClick={focusAuthForm}>
                로그인
              </button>
              <button type="button" className="header-link-button" onClick={() => navigate(LANDING_SIGNUP_PATH)}>
                회원가입
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
