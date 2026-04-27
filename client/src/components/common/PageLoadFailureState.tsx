import { useUiTextValue } from '../../lib/uiText';
import './PageLoadFailureState.css';

const PAGE_LOAD_FAILURE_MESSAGE_KEY = 'COMMON_PAGE_LOAD_FAILURE_MESSAGE';
const PAGE_LOAD_FAILURE_MESSAGE_FALLBACK = '잠시 후 다시 시도해주세요.';

export default function PageLoadFailureState({ className = '', message }: { className?: string; message?: string | null }) {
  const fallbackMessage = useUiTextValue(PAGE_LOAD_FAILURE_MESSAGE_KEY, PAGE_LOAD_FAILURE_MESSAGE_FALLBACK);
  const resolvedMessage = typeof message === 'string' && message.trim() !== '' ? message : fallbackMessage;
  return <div className={`page-load-failure-state ${className}`.trim()}>{resolvedMessage}</div>;
}
