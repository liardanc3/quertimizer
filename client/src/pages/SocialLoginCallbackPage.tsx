import { useEffect } from 'react';
import { PageLoading } from '../components/common/LoadingSpinner';
import { fetchSessionMe } from '../lib/authApi';
import { completeAuthentication } from '../lib/authSession';
import { DASHBOARD_PATH, DEFAULT_PROBLEM_PATH, navigate } from '../lib/navigation';
import {
  SOCIAL_LOGIN_ERROR_MESSAGE,
  SOCIAL_LOGIN_SUCCESS_MESSAGE,
} from '../lib/socialLoginCallback';

function clearSocialLoginCallbackQuery() {
  window.history.replaceState(window.history.state ?? {}, '', `${window.location.pathname}${window.location.hash}`);
}

function postSocialLoginCallbackToOpener(type: string, provider: string | null) {
  window.opener?.postMessage({ type, provider }, '*');
}

export default function SocialLoginCallbackPage() {
  useEffect(() => {
    let isDisposed = false;

    async function handleSocialLoginCallback() {
      const params = new URLSearchParams(window.location.search);
      const socialLoginSuccess = params.get('socialLoginSuccess');
      const socialLoginError = params.get('socialLoginError');
      const isPopupWindow = window.opener != null && !window.opener.closed;

      if (isPopupWindow) {
        postSocialLoginCallbackToOpener(
          socialLoginError == null ? SOCIAL_LOGIN_SUCCESS_MESSAGE : SOCIAL_LOGIN_ERROR_MESSAGE,
          socialLoginSuccess ?? socialLoginError ?? 'oauth2',
        );
        clearSocialLoginCallbackQuery();
        window.close();
        return;
      }

      if (socialLoginError != null) {
        clearSocialLoginCallbackQuery();
        navigate(DASHBOARD_PATH, { replace: true });
        return;
      }

      try {
        const session = await fetchSessionMe();
        if (isDisposed) {
          return;
        }

        await completeAuthentication(session);
        navigate(session.authenticated ? DEFAULT_PROBLEM_PATH : DASHBOARD_PATH, { replace: true });
      } catch {
        navigate(DASHBOARD_PATH, { replace: true });
      } finally {
        clearSocialLoginCallbackQuery();
      }
    }

    void handleSocialLoginCallback();

    return () => {
      isDisposed = true;
    };
  }, []);

  return <PageLoading />;
}

