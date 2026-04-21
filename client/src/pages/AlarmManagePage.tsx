import { useEffect, useRef, useState } from 'react';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { fetchAdminAlarmTemplates, updateAlarmTemplate, type AlarmTemplateData } from '../lib/alarmTemplateApi';

interface EditableAlarmTemplateRow extends AlarmTemplateData {
  originalSentence: string;
  originalDescription: string;
  isEditing: boolean;
  isSaving: boolean;
  errorMessage: string | null;
}

interface ColumnResizeState {
  columnIndex: number;
  startX: number;
  startWidths: number[];
}

const ALARM_TEMPLATE_COLUMN_WEIGHTS = [1.05, 3.2, 2.1];
const ALARM_TEMPLATE_MINIMUM_WEIGHTS = [0.8, 2.3, 1.6];
const ALARM_TEMPLATE_ACTIONS_WIDTH = 68;
const ALARM_TEMPLATE_COLUMN_GAP = 6;

function toEditableRow(alarmTemplate: AlarmTemplateData): EditableAlarmTemplateRow {
  return {
    ...alarmTemplate,
    originalSentence: alarmTemplate.sentence,
    originalDescription: alarmTemplate.description,
    isEditing: false,
    isSaving: false,
    errorMessage: null,
  };
}

function EditIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="M4.2 13.9 3.5 16.5l2.6-.7L14.7 7.2a1.5 1.5 0 0 0 0-2.1l-.8-.8a1.5 1.5 0 0 0-2.1 0Z"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
      <path d="m11.1 4.9 4 4" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.6" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="m4.8 10.4 3.3 3.3 7.1-7.4"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="m6 6 8 8M14 6l-8 8"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  );
}

function getAlarmTemplateGridContentWidth(gridWidth: number) {
  return gridWidth - ALARM_TEMPLATE_ACTIONS_WIDTH - (ALARM_TEMPLATE_COLUMN_WEIGHTS.length * ALARM_TEMPLATE_COLUMN_GAP);
}

function resolveColumnWidths(containerWidth: number, initialWeights: number[]) {
  const totalWeight = initialWeights.reduce((sum, weight) => sum + weight, 0);

  if (containerWidth <= 0 || totalWeight <= 0) {
    return initialWeights.map(() => 132);
  }

  return initialWeights.map((weight) => (weight / totalWeight) * containerWidth);
}

function buildColumnTemplate(columnWidths: number[]) {
  return columnWidths.map((width) => `${width}px`).join(' ');
}

export function AlarmManageContent() {
  const gridRef = useRef<HTMLDivElement | null>(null);
  const [alarmRows, setAlarmRows] = useState<EditableAlarmTemplateRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadErrorMessage, setLoadErrorMessage] = useState<string | null>(null);
  const [columnWidths, setColumnWidths] = useState<number[]>([]);
  const [resizeState, setResizeState] = useState<ColumnResizeState | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadAlarmTemplates() {
      setIsLoading(true);
      setLoadErrorMessage(null);

      try {
        const loadedAlarmTemplates = await fetchAdminAlarmTemplates();
        if (cancelled) {
          return;
        }

        setAlarmRows(loadedAlarmTemplates.map(toEditableRow));
      } catch (error) {
        if (cancelled) {
          return;
        }

        setLoadErrorMessage(error instanceof Error ? error.message : '알람 템플릿 목록을 불러오지 못했다.');
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadAlarmTemplates();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!gridRef.current || isLoading || alarmRows.length === 0) {
      setColumnWidths([]);
      return;
    }

    const containerWidth = getAlarmTemplateGridContentWidth(gridRef.current.getBoundingClientRect().width);
    setColumnWidths(resolveColumnWidths(containerWidth, ALARM_TEMPLATE_COLUMN_WEIGHTS));
  }, [alarmRows.length, isLoading]);

  useEffect(() => {
    if (!resizeState) {
      return;
    }

    const handleMouseMove = (event: MouseEvent) => {
      if (!gridRef.current) {
        return;
      }

      const containerWidth = getAlarmTemplateGridContentWidth(gridRef.current.getBoundingClientRect().width);
      const totalWeight = ALARM_TEMPLATE_COLUMN_WEIGHTS.reduce((sum, weight) => sum + weight, 0);
      const minimumColumnWidths = ALARM_TEMPLATE_MINIMUM_WEIGHTS.map(
        (weight) => (Math.max(containerWidth, 0) * weight) / totalWeight,
      );
      const deltaWidth = event.clientX - resizeState.startX;
      const leftWidth = resizeState.startWidths[resizeState.columnIndex];
      const rightWidth = resizeState.startWidths[resizeState.columnIndex + 1];
      const pairWidth = leftWidth + rightWidth;
      const nextLeftWidth = Math.min(
        Math.max(leftWidth + deltaWidth, minimumColumnWidths[resizeState.columnIndex]),
        pairWidth - minimumColumnWidths[resizeState.columnIndex + 1],
      );
      const nextWidths = [...resizeState.startWidths];

      nextWidths[resizeState.columnIndex] = nextLeftWidth;
      nextWidths[resizeState.columnIndex + 1] = pairWidth - nextLeftWidth;
      setColumnWidths(nextWidths);
    };

    const handleMouseUp = () => {
      setResizeState(null);
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [resizeState]);

  const resolvedColumnWidths =
    columnWidths.length === ALARM_TEMPLATE_COLUMN_WEIGHTS.length
      ? columnWidths
      : resolveColumnWidths(0, ALARM_TEMPLATE_COLUMN_WEIGHTS);
  const columnTemplate = buildColumnTemplate(resolvedColumnWidths);
  const rowTemplate = `${columnTemplate} ${ALARM_TEMPLATE_ACTIONS_WIDTH}px`;
  const rowGap = `${ALARM_TEMPLATE_COLUMN_GAP}px`;
  const rowWidth = `${
    resolvedColumnWidths.reduce((sum, width) => sum + width, 0)
    + ALARM_TEMPLATE_ACTIONS_WIDTH
    + (ALARM_TEMPLATE_COLUMN_WEIGHTS.length * ALARM_TEMPLATE_COLUMN_GAP)
  }px`;

  function renderResizer(columnIndex: number, label: string) {
    if (columnIndex >= ALARM_TEMPLATE_COLUMN_WEIGHTS.length - 1) {
      return null;
    }

    return (
      <button
        type="button"
        className="admin-config-column-resizer"
        aria-label={`${label} 너비 조절`}
        onMouseDown={(event) => {
          event.preventDefault();
          setResizeState({
            columnIndex,
            startX: event.clientX,
            startWidths: resolvedColumnWidths,
          });
        }}
      />
    );
  }

  function handleRowChange(type: string, field: 'sentence' | 'description', value: string) {
    setAlarmRows((currentRows) =>
      currentRows.map((row) =>
        row.type === type
          ? {
              ...row,
              [field]: value,
              errorMessage: null,
            }
          : row,
      ),
    );
  }

  function handleStartEditing(type: string) {
    setAlarmRows((currentRows) =>
      currentRows.map((row) =>
        row.type === type
          ? {
              ...row,
              isEditing: true,
              errorMessage: null,
            }
          : row,
      ),
    );
  }

  function handleCancelEditing(type: string) {
    setAlarmRows((currentRows) =>
      currentRows.map((row) =>
        row.type === type
          ? {
              ...row,
              sentence: row.originalSentence,
              description: row.originalDescription,
              isEditing: false,
              isSaving: false,
              errorMessage: null,
            }
          : row,
      ),
    );
  }

  async function handleSave(type: string) {
    const targetRow = alarmRows.find((row) => row.type === type);
    if (!targetRow) {
      return;
    }

    setAlarmRows((currentRows) =>
      currentRows.map((row) =>
        row.type === type
          ? {
              ...row,
              isSaving: true,
              errorMessage: null,
            }
          : row,
      ),
    );

    try {
      const nextAlarmTemplate = await updateAlarmTemplate(type, {
        sentence: targetRow.sentence,
        description: targetRow.description,
      });

      setAlarmRows((currentRows) =>
        currentRows.map((row) =>
          row.type === type
            ? toEditableRow(nextAlarmTemplate)
            : row,
        ),
      );
    } catch (error) {
      setAlarmRows((currentRows) =>
        currentRows.map((row) =>
          row.type === type
            ? {
                ...row,
                isSaving: false,
                errorMessage: error instanceof Error ? error.message : '알람 템플릿 수정에 실패했다.',
              }
            : row,
        ),
      );
    }
  }

  if (isLoading) {
    return (
      <section className="admin-config-panel">
        <div className="admin-config-toolbar" aria-hidden="true" />

        <div className="admin-page-loading-shell admin-config-loading-shell is-loading" aria-live="polite" aria-label="로딩 중">
          <div className="admin-page-loading-body" aria-hidden="true">
            <div className="admin-page-loading-row is-wide" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row is-narrow" />
          </div>

          <div className="submit-history-loading-overlay" aria-hidden="true">
            <span className="page-loading-spinner submit-history-loading-badge" />
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="admin-config-panel">
      <div className="admin-config-toolbar" aria-hidden="true" />
      {loadErrorMessage ? <PageLoadFailureState className="admin-config-empty" /> : null}

      {loadErrorMessage ? null : alarmRows.length === 0 ? (
        <div className="admin-config-empty">등록된 알람 템플릿이 없다.</div>
      ) : (
        <div className="admin-config-table" ref={gridRef} role="table" aria-label="알람 템플릿 목록">
          <div className="admin-config-header" role="row" style={{ gridTemplateColumns: rowTemplate, columnGap: rowGap, width: rowWidth }}>
            <div className="admin-config-header-cell">
              <span>type</span>
              {renderResizer(0, 'type')}
            </div>
            <div className="admin-config-header-cell">
              <span>sentence</span>
              {renderResizer(1, 'sentence')}
            </div>
            <div className="admin-config-header-cell">
              <span>description</span>
            </div>
            <div className="admin-config-header-cell admin-config-header-cell-actions" aria-hidden="true" />
          </div>

          {alarmRows.map((row) => (
            <div key={row.type} className="admin-config-row" role="row">
              <div className="admin-config-row-grid" style={{ gridTemplateColumns: rowTemplate, columnGap: rowGap, width: rowWidth }}>
                <div className="admin-config-field">
                  <span className="admin-config-field-label">type</span>
                  <div className="admin-config-display">{row.type}</div>
                  {renderResizer(0, 'type')}
                </div>

                <div className="admin-config-field">
                  <span className="admin-config-field-label">sentence</span>
                  {row.isEditing ? (
                    <textarea
                      className="text-field admin-config-textarea"
                      value={row.sentence}
                      onChange={(event) => handleRowChange(row.type, 'sentence', event.target.value)}
                    />
                  ) : (
                    <div className="admin-config-display admin-config-display-multiline">{row.sentence}</div>
                  )}
                  {renderResizer(1, 'sentence')}
                </div>

                <div className="admin-config-field">
                  <span className="admin-config-field-label">description</span>
                  {row.isEditing ? (
                    <input
                      type="text"
                      className="text-field admin-config-input"
                      value={row.description}
                      onChange={(event) => handleRowChange(row.type, 'description', event.target.value)}
                    />
                  ) : (
                    <div className="admin-config-display admin-config-display-multiline">{row.description}</div>
                  )}
                </div>

                <div className="admin-config-actions">
                  {row.isEditing ? (
                    <>
                      <button
                        type="button"
                        className="btn text admin-config-icon-button"
                        onClick={() => void handleSave(row.type)}
                        disabled={row.isSaving}
                        aria-label={`${row.type} 저장`}
                      >
                        <CheckIcon />
                      </button>
                      <button
                        type="button"
                        className="btn text admin-config-icon-button"
                        onClick={() => handleCancelEditing(row.type)}
                        disabled={row.isSaving}
                        aria-label={`${row.type} 수정 취소`}
                      >
                        <CloseIcon />
                      </button>
                    </>
                  ) : (
                    <button
                      type="button"
                      className="btn text admin-config-icon-button"
                      onClick={() => handleStartEditing(row.type)}
                      aria-label={`${row.type} 수정`}
                    >
                      <EditIcon />
                    </button>
                  )}
                </div>
              </div>

              {row.errorMessage ? <p className="admin-config-feedback is-error">{row.errorMessage}</p> : null}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
