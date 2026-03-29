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
    <div className="problem-control-group problem-status-group" role="group" aria-label="해결 상태 필터">
      <span className="problem-control-label">상태</span>
      <div className="problem-status-buttons">
        <button
          type="button"
          className={`mini-toggle problem-status-button ${showSolved ? 'is-selected' : ''}`}
          aria-pressed={showSolved}
          onClick={onToggleSolved}
        >
          해결
        </button>
        <button
          type="button"
          className={`mini-toggle problem-status-button ${showUnsolved ? 'is-selected' : ''}`}
          aria-pressed={showUnsolved}
          onClick={onToggleUnsolved}
        >
          미해결
        </button>
      </div>
    </div>
  );
}
