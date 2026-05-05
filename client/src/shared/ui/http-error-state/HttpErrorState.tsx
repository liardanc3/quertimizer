import { resolveCommonHttpErrorFallback, resolveCommonHttpErrorMessageKey } from '@/shared/api/api-error';
import { useSession } from '@/shared/auth/session';
import { useUiText } from '@/shared/config/ui-text';
import '@/shared/ui/http-error-state/HttpErrorState.css';

interface HttpErrorStateProps {
  status?: number | null;
  className?: string;
  message?: string | null;
}

export default function HttpErrorState({ status, className = '', message }: HttpErrorStateProps) {
  const session = useSession();
  const { text } = useUiText();

  if (status === 401 || (status === 403 && !session.isAuthenticated)) {
    return null;
  }

  const resolvedMessage = typeof message === 'string' && message.trim() !== ''
    ? message
    : text(resolveCommonHttpErrorMessageKey(status), resolveCommonHttpErrorFallback(status));

  return (
    <div className={`http-error-state ${className}`.trim()} role="status">
      {resolvedMessage}
    </div>
  );
}
