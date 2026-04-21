import './PageLoadFailureState.css';

export const PAGE_LOAD_FAILURE_MESSAGE = '잠시 후 다시 시도해주세요.';

export default function PageLoadFailureState({ className = '' }: { className?: string }) {
  return <div className={`page-load-failure-state ${className}`.trim()}>{PAGE_LOAD_FAILURE_MESSAGE}</div>;
}
