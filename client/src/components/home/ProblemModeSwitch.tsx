import type { ProblemViewMode } from '../../types/domain';

interface ProblemModeSwitchProps {
  mode: ProblemViewMode;
  onChange: (mode: ProblemViewMode) => void;
}

export default function ProblemModeSwitch({ mode, onChange }: ProblemModeSwitchProps) {
  return (
    <div className="segmented" role="group" aria-label="문제 보기 모드">
      <button type="button" className={`segmented-btn ${mode === 'tagged' ? 'is-selected' : ''}`} onClick={() => onChange('tagged')}>
        태그 표시
      </button>
      <button
        type="button"
        className={`segmented-btn ${mode === 'spoilerFree' ? 'is-selected' : ''}`}
        onClick={() => onChange('spoilerFree')}
      >
        스포일러 방지
      </button>
    </div>
  );
}
