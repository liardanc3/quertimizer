import './SessionToast.css';

interface SessionToastProps {
  open: boolean;
  message: string;
  tone?: 'success' | 'error';
}

function CheckIcon() {
  return (
    <svg className="session-toast-icon" viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="M5 10.2 8.2 13.4 15 6.6"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.9"
      />
    </svg>
  );
}

function ErrorIcon() {
  return (
    <svg className="session-toast-icon" viewBox="0 0 20 20" aria-hidden="true">
      <path d="M10 6v4.8" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.9" />
      <path d="M10 14.2h.01" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.2" />
      <path
        d="M10 2.8 17.2 16a1 1 0 0 1-.88 1.5H3.68A1 1 0 0 1 2.8 16L10 2.8Z"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.55"
      />
    </svg>
  );
}

export default function SessionToast({ open, message, tone = 'success' }: SessionToastProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="session-toast-shell" aria-live="polite" aria-atomic="true">
      <div className={`session-toast ${tone === 'error' ? 'is-error' : 'is-success'}`.trim()} role="status">
        <span className="session-toast-icon-shell" aria-hidden="true">
          {tone === 'error' ? <ErrorIcon /> : <CheckIcon />}
        </span>
        <p className="session-toast-message">{message}</p>
      </div>
    </div>
  );
}
