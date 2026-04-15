import { useEffect, useState, type CSSProperties, type FocusEvent, type KeyboardEvent } from 'react';

interface ProblemSpreadRateFilterProps {
  minBound: number;
  maxBound: number;
  selectedMin: number;
  selectedMax: number;
  displayMin: number;
  displayMax: number;
  sortOrder: 'none' | 'asc' | 'desc';
  onToggleSort: () => void;
  onChangeMin: (value: number) => void;
  onChangeMax: (value: number) => void;
  onChangeRange: (range: { min: number; max: number }) => void;
  onApplyRange: (range?: { min: number; max: number }) => void;
  hasPendingChanges: boolean;
}

function SortNeutralIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 2.6v4.25" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <path d="M5.45 5.15 8 2.6l2.55 2.55" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M8 13.4V9.15" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <path d="m5.45 10.85 2.55 2.55 2.55-2.55" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function SortAscendingIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 2.5v10.9" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      <path d="M5.2 5.25 8 2.5l2.8 2.75" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function SortDescendingIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 2.6v10.9" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      <path d="m5.2 10.75 2.8 2.75 2.8-2.75" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ApplyRangeIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path
        d="M12.95 6.2A4.95 4.95 0 0 0 4.78 3.85"
        stroke="currentColor"
        strokeWidth="1.55"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M4.75 1.8v2.55H7.3"
        stroke="currentColor"
        strokeWidth="1.55"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M3.05 9.8A4.95 4.95 0 0 0 11.22 12.15"
        stroke="currentColor"
        strokeWidth="1.55"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M11.25 14.2v-2.55H8.7"
        stroke="currentColor"
        strokeWidth="1.55"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function formatPercent(value: number) {
  const normalizedValue = Math.round(value * 10) / 10;
  return Number.isInteger(normalizedValue) ? `${normalizedValue}%` : `${normalizedValue.toFixed(1)}%`;
}

function formatRangeLabel(min: number, max: number) {
  return `${formatPercent(min)}\u00A0\u00A0~\u00A0\u00A0${formatPercent(max)}`;
}

function formatDraftValue(value: number) {
  const normalizedValue = Math.round(value * 10) / 10;
  return Number.isInteger(normalizedValue) ? String(normalizedValue) : normalizedValue.toFixed(1);
}

function clampPercentValue(value: number) {
  return Math.min(100, Math.max(0, value));
}

function isValidPercentDraft(value: string) {
  if (value === '') {
    return true;
  }

  if (value === '100') {
    return true;
  }

  if (value.startsWith('100.')) {
    return false;
  }

  return /^(?:\d{1,2}(?:\.\d?)?)$/.test(value);
}

export default function ProblemSpreadRateFilter({
  minBound,
  maxBound,
  selectedMin,
  selectedMax,
  displayMin,
  displayMax,
  sortOrder,
  onToggleSort,
  onChangeMin,
  onChangeMax,
  onChangeRange,
  onApplyRange,
  hasPendingChanges,
}: ProblemSpreadRateFilterProps) {
  const [isEditingValue, setIsEditingValue] = useState(false);
  const [draftMin, setDraftMin] = useState(formatDraftValue(displayMin));
  const [draftMax, setDraftMax] = useState(formatDraftValue(displayMax));
  const isDisabled = minBound === maxBound;
  const span = Math.max(maxBound - minBound, 1);
  const minHandlePercent = ((selectedMin - minBound) / span) * 100;
  const maxHandlePercent = ((selectedMax - minBound) / span) * 100;
  const activeStartPercent = Math.min(minHandlePercent, maxHandlePercent);
  const activeEndPercent = Math.max(minHandlePercent, maxHandlePercent);
  const sliderStyle = {
    '--problem-variance-start': `${activeStartPercent}%`,
    '--problem-variance-end': `${activeEndPercent}%`,
  } as CSSProperties;
  const sortLabel =
    sortOrder === 'asc'
      ? 'Cost \uD3B8\uCC28 \uC624\uB984\uCC28\uC21C'
      : sortOrder === 'desc'
        ? 'Cost \uD3B8\uCC28 \uB0B4\uB9BC\uCC28\uC21C'
        : 'Cost \uD3B8\uCC28 \uC815\uB82C \uC5C6\uC74C';

  useEffect(() => {
    if (isEditingValue) {
      return;
    }

    setDraftMin(formatDraftValue(displayMin));
    setDraftMax(formatDraftValue(displayMax));
  }, [displayMax, displayMin, isEditingValue]);

  function resolveDraftRange() {
    const parsedMin = Number.parseFloat(draftMin);
    const parsedMax = Number.parseFloat(draftMax);
    const resolvedMin = Number.isNaN(parsedMin) ? displayMin : clampPercentValue(parsedMin);
    const resolvedMax = Number.isNaN(parsedMax) ? displayMax : clampPercentValue(parsedMax);

    return {
      min: Math.min(resolvedMin, resolvedMax),
      max: Math.max(resolvedMin, resolvedMax),
    };
  }

  function applyDraftRange() {
    const nextRange = resolveDraftRange();

    onChangeRange(nextRange);
    setDraftMin(formatDraftValue(nextRange.min));
    setDraftMax(formatDraftValue(nextRange.max));
    setIsEditingValue(false);

    return nextRange;
  }

  function resetDraftRange() {
    setDraftMin(formatDraftValue(displayMin));
    setDraftMax(formatDraftValue(displayMax));
    setIsEditingValue(false);
  }

  function handleDraftMinChange(nextValue: string) {
    if (!isValidPercentDraft(nextValue)) {
      return;
    }

    setDraftMin(nextValue);
  }

  function handleDraftMaxChange(nextValue: string) {
    if (!isValidPercentDraft(nextValue)) {
      return;
    }

    setDraftMax(nextValue);
  }

  function handleApplyClick() {
    if (isEditingValue) {
      onApplyRange(applyDraftRange());
      return;
    }

    onApplyRange();
  }

  function handleEditorBlur(event: FocusEvent<HTMLDivElement>) {
    const nextTarget = event.relatedTarget;
    if (nextTarget instanceof HTMLElement && nextTarget.dataset.spreadApplyButton === 'true') {
      return;
    }

    if (nextTarget instanceof Node && event.currentTarget.contains(nextTarget)) {
      return;
    }

    applyDraftRange();
  }

  function handleEditorKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      event.preventDefault();
      applyDraftRange();
      return;
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      resetDraftRange();
    }
  }

  const draftRange = isEditingValue ? resolveDraftRange() : null;
  const hasDraftChanges =
    draftRange != null && (draftRange.min !== displayMin || draftRange.max !== displayMax);
  const canApplyChanges = !isDisabled && (hasPendingChanges || hasDraftChanges);

  return (
    <div
      className="problem-control-group problem-variance-group"
      role="group"
      aria-label={`Cost \uD3B8\uCC28 \uD544\uD130`}
    >
      <div className="problem-variance-heading">
        <span className="problem-control-label">{`Cost \uD3B8\uCC28`}</span>
        <button
          type="button"
          className={`problem-variance-apply-button ${canApplyChanges ? 'is-pending' : ''}`.trim()}
          data-spread-apply-button="true"
          aria-label={`Cost \uD3B8\uCC28 \uC801\uC6A9`}
          title={`Cost \uD3B8\uCC28 \uC801\uC6A9`}
          disabled={!canApplyChanges}
          onClick={handleApplyClick}
        >
          <ApplyRangeIcon />
        </button>
      </div>

      <div className="problem-variance-controls">
        <button
          type="button"
          className={`problem-sort-toggle-button ${sortOrder === 'none' ? '' : 'is-selected'}`.trim()}
          aria-label={sortLabel}
          title={sortLabel}
          aria-pressed={sortOrder !== 'none'}
          onClick={onToggleSort}
        >
          {sortOrder === 'asc' ? <SortAscendingIcon /> : sortOrder === 'desc' ? <SortDescendingIcon /> : <SortNeutralIcon />}
        </button>

        <div className="problem-variance-slider-shell" style={sliderStyle}>
          <div className="problem-variance-slider-track" aria-hidden="true">
            <span className="problem-variance-slider-active" />
          </div>

          <input
            type="range"
            min={minBound}
            max={maxBound}
            step={1}
            value={selectedMin}
            className="problem-variance-slider-input is-min"
            aria-label={`Cost \uD3B8\uCC28 \uCD5C\uC18C\uAC12`}
            disabled={isDisabled}
            onChange={(event) => onChangeMin(Number(event.target.value))}
          />

          <input
            type="range"
            min={minBound}
            max={maxBound}
            step={1}
            value={selectedMax}
            className="problem-variance-slider-input is-max"
            aria-label={`Cost \uD3B8\uCC28 \uCD5C\uB300\uAC12`}
            disabled={isDisabled}
            onChange={(event) => onChangeMax(Number(event.target.value))}
          />
        </div>

        <div className="problem-variance-actions">
          {isEditingValue ? (
            <div className="problem-variance-value-editor" onBlur={handleEditorBlur}>
              <input
                type="text"
                inputMode="decimal"
                className="problem-variance-value-input"
                aria-label={`Cost \uD3B8\uCC28 \uCD5C\uC18C \uC785\uB825\uAC12`}
                value={draftMin}
                onChange={(event) => handleDraftMinChange(event.target.value)}
                onKeyDown={handleEditorKeyDown}
                autoFocus
              />
              <span className="problem-variance-value-unit">%</span>
              <span className="problem-variance-value-separator">~</span>
              <input
                type="text"
                inputMode="decimal"
                className="problem-variance-value-input"
                aria-label={`Cost \uD3B8\uCC28 \uCD5C\uB300 \uC785\uB825\uAC12`}
                value={draftMax}
                onChange={(event) => handleDraftMaxChange(event.target.value)}
                onKeyDown={handleEditorKeyDown}
              />
              <span className="problem-variance-value-unit">%</span>
            </div>
          ) : (
            <button
              type="button"
              className="problem-variance-value-button"
              aria-label={`Cost \uD3B8\uCC28 \uBC94\uC704 \uC9C1\uC811 \uC785\uB825`}
              onClick={() => setIsEditingValue(true)}
            >
              {formatRangeLabel(displayMin, displayMax)}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
