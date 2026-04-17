import { useEffect } from 'react';
import StatusPopup from './components/common/StatusPopup';
import MainLayout from './layouts/MainLayout';
import { useAuthenticationSocket } from './lib/authSession';
import { useSessionAlert } from './lib/session';
import { preloadUiTexts } from './lib/uiText';
import AppRouter from './router';

export default function App() {
  useAuthenticationSocket();
  const { sessionAlert, dismissSessionAlert } = useSessionAlert();

  useEffect(() => {
    void preloadUiTexts();
  }, []);

  function handleSessionAlertConfirm() {
    dismissSessionAlert();
  }

  return (
    <>
      <MainLayout>
        <AppRouter />
      </MainLayout>

      <StatusPopup
        open={sessionAlert != null}
        level={sessionAlert?.level ?? 1}
        message={sessionAlert?.message ?? ''}
        confirmLabel={sessionAlert?.confirmLabel ?? '확인'}
        onConfirm={handleSessionAlertConfirm}
      />
    </>
  );
}
