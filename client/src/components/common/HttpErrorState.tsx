import { resolveCommonHttpErrorFallback, resolveCommonHttpErrorMessageKey } from '../../lib/apiError';
import { useUiText } from '../../lib/uiText';
import './HttpErrorState.css';

interface HttpErrorStateProps {
  status?: number | null;
  className?: string;
  message?: string | null;
}

export default function HttpErrorState({ status, className = '', message }: HttpErrorStateProps) {
  const { text } = useUiText();
  const resolvedMessage = typeof message === 'string' && message.trim() !== ''
    ? message
    : text(resolveCommonHttpErrorMessageKey(status), resolveCommonHttpErrorFallback(status));

  return (
    <div className={`http-error-state ${className}`.trim()} role="status">
      {resolvedMessage}
    </div>
  );
}
