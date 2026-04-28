import HttpErrorState from './HttpErrorState';
import PageLoadFailureState from './PageLoadFailureState';

interface PageErrorStateProps {
  status?: number | null;
  message?: string | null;
  className?: string;
}

export default function PageErrorState({ status, message, className = '' }: PageErrorStateProps) {
  return status != null
    ? <HttpErrorState status={status} className={className} message={message} />
    : <PageLoadFailureState className={className} message={message} />;
}
