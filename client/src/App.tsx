import { useEffect } from 'react';
import SessionToast from './components/common/SessionToast';
import StatusPopup from './components/common/StatusPopup';
import MainLayout from './layouts/MainLayout';
import { useAuthenticationSocket } from './lib/authSession';
import { useSessionAlert } from './lib/session';
import { preloadUiTexts, useUiText } from './lib/uiText';
import AppRouter from './router';

export default function App() {
  useAuthenticationSocket();
  const { text } = useUiText();
  const { sessionAlert, dismissSessionAlert } = useSessionAlert();

  useEffect(() => {
    void preloadUiTexts();
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

  function handleSessionAlertConfirm() {
    dismissSessionAlert();
  }

  return (
    <>
      <MainLayout>
        <AppRouter />
      </MainLayout>

      <SessionToast open={sessionAlert?.display === 'toast'} message={sessionAlert?.message ?? ''} tone={sessionAlert?.tone} />

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
