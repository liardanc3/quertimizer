import { useEffect } from 'react';
import { SessionToast } from '@/shared/ui';
import { StatusPopup } from '@/shared/ui';
import { MainLayout } from '@/widgets/main-layout';
import { useAuthenticationSocket } from '@/shared/auth/auth-session';
import { useSessionAlert } from '@/shared/auth/session';
import { preloadUiTexts, useUiText } from '@/shared/config/ui-text';
import { shouldSuppressClickForTextSelection } from '@/shared/lib/text-selection';
import { useRateLimitNotice } from '@/shared/lib/rate-limit-notice';
import AppRouter from '@/app/router';

export default function App() {
  useAuthenticationSocket();
  const { text } = useUiText();
  const { sessionAlert, dismissSessionAlert } = useSessionAlert();
  const { rateLimitNotice, dismissRateLimitNotice } = useRateLimitNotice();
  const activeToast = rateLimitNotice != null
    ? { message: rateLimitNotice.message, tone: 'error' as const }
    : sessionAlert?.display === 'toast'
      ? { message: sessionAlert.message, tone: sessionAlert.tone }
      : null;

  useEffect(() => {
    void preloadUiTexts();
  }, []);

  useEffect(() => {
    function handleDocumentClick(event: MouseEvent) {
      if (!shouldSuppressClickForTextSelection(event)) {
        return;
      }

      event.preventDefault();
      event.stopPropagation();
      event.stopImmediatePropagation();
    }

    document.addEventListener('click', handleDocumentClick, true);
    return () => document.removeEventListener('click', handleDocumentClick, true);
  }, []);

  useEffect(() => {
    if (sessionAlert == null || sessionAlert.display === 'popup') {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      dismissSessionAlert();
    }, sessionAlert.autoDismissMs ?? 2200);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [dismissSessionAlert, sessionAlert]);

  useEffect(() => {
    if (rateLimitNotice == null) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      dismissRateLimitNotice();
    }, rateLimitNotice.autoDismissMs);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [dismissRateLimitNotice, rateLimitNotice]);

  function handleSessionAlertConfirm() {
    dismissSessionAlert();
  }

  return (
    <>
      <MainLayout>
        <AppRouter />
      </MainLayout>

      <SessionToast open={activeToast != null} message={activeToast?.message ?? ''} tone={activeToast?.tone} />

      <StatusPopup
        open={sessionAlert != null && sessionAlert.display !== 'toast'}
        level={sessionAlert?.level ?? 1}
        message={sessionAlert?.message ?? ''}
        confirmLabel={sessionAlert?.confirmLabel ?? text('COMMON_CONFIRM_BUTTON', '확인')}
        onConfirm={handleSessionAlertConfirm}
      />
    </>
  );
}
