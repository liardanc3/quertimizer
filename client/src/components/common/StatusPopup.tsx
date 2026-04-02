interface StatusPopupProps {
  open: boolean;
  level: 1 | 2 | 3;
  message: string;
  confirmLabel?: string;
  onConfirm: () => void;
}

export default function StatusPopup({
  open,
  level,
  message,
  confirmLabel = '확인',
  onConfirm,
}: StatusPopupProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="status-popup-scrim" role="dialog" aria-modal="true" aria-label="상태 안내">
      <div className={`status-popup status-popup-level-${level}`}>
        <div className={`status-popup-visual status-popup-visual-level-${level}`} aria-hidden="true">
          <StatusPopupIcon level={level} />
        </div>

        <p className="status-popup-message">{message}</p>

        <button type="button" className="btn primary status-popup-button" onClick={onConfirm}>
          {confirmLabel}
        </button>
      </div>
    </div>
  );
}

function StatusPopupIcon({ level }: { level: 1 | 2 | 3 }) {
  if (level === 1) {
    return (
      <svg className="status-popup-icon" viewBox="0 0 64 64">
        <circle cx="32" cy="32" r="24" fill="currentColor" opacity="0.16" />
        <path
          d="M26.5 40.8 18.9 33.2a3 3 0 1 1 4.2-4.2l5.5 5.5L40.9 22a3 3 0 1 1 4.2 4.2L30.7 40.8a3 3 0 0 1-4.2 0Z"
          fill="currentColor"
        />
      </svg>
    );
  }

  if (level === 2) {
    return (
      <svg className="status-popup-icon" viewBox="0 0 64 64">
        <path
          d="M28.9 10.8a3.6 3.6 0 0 1 6.2 0l21.5 37.4a3.6 3.6 0 0 1-3.1 5.4H10.5a3.6 3.6 0 0 1-3.1-5.4l21.5-37.4Z"
          fill="currentColor"
          opacity="0.18"
        />
        <path d="M32 22.5a3 3 0 0 1 3 3V35a3 3 0 0 1-6 0v-9.5a3 3 0 0 1 3-3Z" fill="currentColor" />
        <circle cx="32" cy="43.8" r="3.3" fill="currentColor" />
      </svg>
    );
  }

  return (
    <svg className="status-popup-icon" viewBox="0 0 64 64">
      <path
        d="M24.1 7.8h15.8l11.2 11.1v15.9L39.9 46H24.1L12.9 34.8V18.9L24.1 7.8Z"
        fill="currentColor"
        opacity="0.18"
      />
      <path d="M32 20.4a3 3 0 0 1 3 3v10.2a3 3 0 0 1-6 0V23.4a3 3 0 0 1 3-3Z" fill="currentColor" />
      <circle cx="32" cy="41.9" r="3.4" fill="currentColor" />
    </svg>
  );
}
