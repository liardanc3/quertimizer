import { resolveCommonHttpErrorFallback, resolveCommonHttpErrorMessageKey } from '../../lib/apiError';
import { useSession } from '../../lib/session';
import { useUiText } from '../../lib/uiText';
import './HttpErrorState.css';

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
