import './SessionToast.css';

interface SessionToastProps {
  open: boolean;
  message: string;
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

export default function SessionToast({ open, message }: SessionToastProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="session-toast-shell" aria-live="polite" aria-atomic="true">
      <div className="session-toast" role="status">
        <span className="session-toast-icon-shell" aria-hidden="true">
          <CheckIcon />
        </span>
        <p className="session-toast-message">{message}</p>
      </div>
    </div>
  );
}
