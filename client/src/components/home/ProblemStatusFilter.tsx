interface ProblemStatusFilterProps {
  showSolved: boolean;
  showUnsolved: boolean;
  onToggleSolved: () => void;
  onToggleUnsolved: () => void;
}

export default function ProblemStatusFilter({
  showSolved,
  showUnsolved,
  onToggleSolved,
  onToggleUnsolved,
}: ProblemStatusFilterProps) {
  return (
    <div
      className="problem-control-group problem-status-group"
      role="group"
      aria-label={'\uC0C1\uD0DC \uD544\uD130'}
    >
      <div className="problem-status-checks">
        <label className="problem-status-check">
          <input
            type="checkbox"
            checked={showSolved}
            onChange={onToggleSolved}
            className="problem-status-check-input"
            aria-label={'\uD574\uACB0'}
          />
          <span className="problem-status-check-text">{'\uD574\uACB0'}</span>
          <span className="problem-status-check-ui" aria-hidden="true" />
        </label>

        <label className="problem-status-check">
          <input
            type="checkbox"
            checked={showUnsolved}
            onChange={onToggleUnsolved}
            className="problem-status-check-input"
            aria-label={'\uBBF8\uD574\uACB0'}
          />
          <span className="problem-status-check-text">{'\uBBF8\uD574\uACB0'}</span>
          <span className="problem-status-check-ui" aria-hidden="true" />
        </label>
      </div>
    </div>
  );
}
