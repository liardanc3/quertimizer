import { useEffect, useRef, useState } from 'react';
import { deleteUiText, fetchAdminUiTexts, updateUiText, type UiTextData } from '../lib/uiText';

interface EditableUiTextRow extends UiTextData {
  originalKey: string;
  originalLanguage: string;
  originalValue: string;
  originalDescription: string;
  isEditing: boolean;
  isSaving: boolean;
  errorMessage: string | null;
}

type UiTextField = keyof UiTextData;

interface ColumnResizeState {
  columnIndex: number;
  startX: number;
  startWidths: number[];
}

const ADMIN_CONFIG_COLUMN_WEIGHTS = [0.95, 3.7, 0.85, 3.3];
const ADMIN_CONFIG_MINIMUM_WEIGHTS = [0.78, 2, 0.72, 1.85];
const ADMIN_CONFIG_ACTIONS_WIDTH = 68;
const ADMIN_CONFIG_COLUMN_GAP = 6;

function toEditableRow(uiText: UiTextData): EditableUiTextRow {
  return {
    ...uiText,
    originalKey: uiText.key,
    originalLanguage: uiText.language,
    originalValue: uiText.value,
    originalDescription: uiText.description,
    isEditing: false,
    isSaving: false,
    errorMessage: null,
  };
}

function sortUiTextRows(rows: EditableUiTextRow[]) {
  return [...rows].sort(
    (left, right) =>
      left.key.localeCompare(right.key) ||
      left.language.localeCompare(right.language) ||
      left.value.localeCompare(right.value),
  );
}

function isSameRow(row: EditableUiTextRow, key: string, language: string) {
  return row.originalKey === key && row.originalLanguage === language;
}

function isDeletableLanguage(language: string) {
  return language !== 'default' && language !== 'kr';
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

function DeleteIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="M6.8 6.4v7m3.2-7v7m3.2-7v7M4.8 5.1h10.4M7.8 3.8h4.4m-6.3 1.3.5 10a1.4 1.4 0 0 0 1.4 1.3h4.4a1.4 1.4 0 0 0 1.4-1.3l.5-10"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.5"
      />
    </svg>
  );
}

function resizeTextareaHeight(textarea: HTMLTextAreaElement) {
  textarea.style.height = '2rem';
  textarea.style.height = `${Math.max(textarea.scrollHeight, 32)}px`;
}

function buildColumnTemplate(columnWidths: number[]) {
  return columnWidths.map((width) => `${width}px`).join(' ');
}

function getConfigGridContentWidth(gridWidth: number) {
  return gridWidth - ADMIN_CONFIG_ACTIONS_WIDTH - (ADMIN_CONFIG_COLUMN_WEIGHTS.length * ADMIN_CONFIG_COLUMN_GAP);
}

function resolveColumnWidths(containerWidth: number, initialWeights: number[]) {
  const totalWeight = initialWeights.reduce((sum, weight) => sum + weight, 0);

  if (containerWidth <= 0 || totalWeight <= 0) {
    return initialWeights.map(() => 132);
  }

  return initialWeights.map((weight) => (weight / totalWeight) * containerWidth);
}

export function GlobalConfigContent() {
  const gridRef = useRef<HTMLDivElement | null>(null);
  const [uiTextRows, setUiTextRows] = useState<EditableUiTextRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadErrorMessage, setLoadErrorMessage] = useState<string | null>(null);
  const [columnWidths, setColumnWidths] = useState<number[]>([]);
  const [resizeState, setResizeState] = useState<ColumnResizeState | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadUiTexts() {
      setIsLoading(true);
      setLoadErrorMessage(null);

      try {
        const loadedUiTexts = await fetchAdminUiTexts();

        if (cancelled) {
          return;
        }

        setUiTextRows(sortUiTextRows(loadedUiTexts.map(toEditableRow)));
      } catch (error) {
        if (cancelled) {
          return;
        }

        setLoadErrorMessage(error instanceof Error ? error.message : 'UI 텍스트 목록을 불러오지 못했다.');
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadUiTexts();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const animationFrameId = window.requestAnimationFrame(() => {
      document.querySelectorAll<HTMLTextAreaElement>('.admin-config-textarea').forEach(resizeTextareaHeight);
    });

    return () => {
      window.cancelAnimationFrame(animationFrameId);
    };
  }, [uiTextRows]);

  useEffect(() => {
    if (!gridRef.current || isLoading || uiTextRows.length === 0) {
      setColumnWidths([]);
      return;
    }

    const containerWidth = getConfigGridContentWidth(gridRef.current.getBoundingClientRect().width);
    setColumnWidths(resolveColumnWidths(containerWidth, ADMIN_CONFIG_COLUMN_WEIGHTS));
  }, [isLoading, uiTextRows.length]);

  useEffect(() => {
    if (!resizeState) {
      return;
    }

    const handleMouseMove = (event: MouseEvent) => {
      if (!gridRef.current) {
        return;
      }

      const containerWidth = getConfigGridContentWidth(gridRef.current.getBoundingClientRect().width);
      const totalWeight = ADMIN_CONFIG_COLUMN_WEIGHTS.reduce((sum, weight) => sum + weight, 0);
      const minimumColumnWidths = ADMIN_CONFIG_MINIMUM_WEIGHTS.map(
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

  function handleRowChange(originalKey: string, originalLanguage: string, field: UiTextField, value: string) {
    setUiTextRows((currentRows) =>
      currentRows.map((row) =>
        isSameRow(row, originalKey, originalLanguage)
          ? {
              ...row,
              [field]: value,
              errorMessage: null,
            }
          : row,
      ),
    );
  }

  function handleStartEditing(originalKey: string, originalLanguage: string) {
    setUiTextRows((currentRows) =>
      currentRows.map((row) =>
        isSameRow(row, originalKey, originalLanguage)
          ? {
              ...row,
              isEditing: true,
              errorMessage: null,
            }
          : row,
      ),
    );
  }

  function handleCancelEditing(originalKey: string, originalLanguage: string) {
    setUiTextRows((currentRows) =>
      currentRows.map((row) =>
        isSameRow(row, originalKey, originalLanguage)
          ? {
              ...row,
              key: row.originalKey,
              value: row.originalValue,
              language: row.originalLanguage,
              description: row.originalDescription,
              isEditing: false,
              isSaving: false,
              errorMessage: null,
            }
          : row,
      ),
    );
  }

  async function handleUpdateUiText(originalKey: string, originalLanguage: string) {
    const targetRow = uiTextRows.find((row) => isSameRow(row, originalKey, originalLanguage));
    if (!targetRow) {
      return;
    }

    setUiTextRows((currentRows) =>
      currentRows.map((row) =>
        isSameRow(row, originalKey, originalLanguage)
          ? {
              ...row,
              isEditing: true,
              isSaving: true,
              errorMessage: null,
            }
          : row,
      ),
    );

    try {
      const updatedUiText = await updateUiText(originalKey, originalLanguage, {
        key: targetRow.key,
        value: targetRow.value,
        language: targetRow.language,
        description: targetRow.description,
      });

      setUiTextRows((currentRows) =>
        sortUiTextRows(
          currentRows.map((row) =>
            isSameRow(row, originalKey, originalLanguage) ? toEditableRow(updatedUiText) : row,
          ),
        ),
      );
    } catch (error) {
      setUiTextRows((currentRows) =>
        currentRows.map((row) =>
          isSameRow(row, originalKey, originalLanguage)
            ? {
                ...row,
                isEditing: true,
                isSaving: false,
                errorMessage: error instanceof Error ? error.message : 'UI 텍스트를 수정하지 못했다.',
              }
            : row,
        ),
      );
    }
  }

  async function handleDeleteUiText(originalKey: string, originalLanguage: string) {
    setUiTextRows((currentRows) =>
      currentRows.map((row) =>
        isSameRow(row, originalKey, originalLanguage)
          ? {
              ...row,
              isSaving: true,
              errorMessage: null,
            }
          : row,
      ),
    );

    try {
      await deleteUiText(originalKey, originalLanguage);

      setUiTextRows((currentRows) =>
        currentRows.filter((row) => !isSameRow(row, originalKey, originalLanguage)),
      );
    } catch (error) {
      setUiTextRows((currentRows) =>
        currentRows.map((row) =>
          isSameRow(row, originalKey, originalLanguage)
            ? {
                ...row,
                isSaving: false,
                errorMessage: error instanceof Error ? error.message : 'UI 텍스트를 삭제하지 못했다.',
              }
            : row,
        ),
      );
    }
  }

  const resolvedColumnWidths =
    columnWidths.length === ADMIN_CONFIG_COLUMN_WEIGHTS.length
      ? columnWidths
      : resolveColumnWidths(0, ADMIN_CONFIG_COLUMN_WEIGHTS);
  const columnTemplate = buildColumnTemplate(resolvedColumnWidths);
  const rowTemplate = `${columnTemplate} ${ADMIN_CONFIG_ACTIONS_WIDTH}px`;
  const rowGap = `${ADMIN_CONFIG_COLUMN_GAP}px`;
  const rowWidth = `${
    resolvedColumnWidths.reduce((sum, width) => sum + width, 0)
    + ADMIN_CONFIG_ACTIONS_WIDTH
    + (ADMIN_CONFIG_COLUMN_WEIGHTS.length * ADMIN_CONFIG_COLUMN_GAP)
  }px`;

  function renderResizer(columnIndex: number, label: string) {
    if (columnIndex >= ADMIN_CONFIG_COLUMN_WEIGHTS.length - 1) {
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

  return (
    <section className="admin-config-panel">

      {loadErrorMessage ? <p className="admin-config-feedback is-error">{loadErrorMessage}</p> : null}

      {isLoading ? (
        <p className="content-text">UI 텍스트 목록을 불러오는 중이다.</p>
      ) : uiTextRows.length === 0 ? (
        <div className="admin-config-empty">등록된 UI 텍스트가 없다.</div>
      ) : (
        <div className="admin-config-table" ref={gridRef} role="table" aria-label="UI 텍스트 목록">
          <div className="admin-config-header" role="row" style={{ gridTemplateColumns: rowTemplate, columnGap: rowGap, width: rowWidth }}>
            <div className="admin-config-header-cell">
              <span>key</span>
              {renderResizer(0, 'key')}
            </div>
            <div className="admin-config-header-cell">
              <span>value</span>
              {renderResizer(1, 'value')}
            </div>
            <div className="admin-config-header-cell">
              <span>language</span>
              {renderResizer(2, 'language')}
            </div>
            <div className="admin-config-header-cell">
              <span>description</span>
            </div>
            <div className="admin-config-header-cell admin-config-header-cell-actions" aria-hidden="true" />
          </div>

          {uiTextRows.map((row) => (
            <div key={`${row.originalKey}:${row.originalLanguage}`} className="admin-config-row" role="row">
              <div className="admin-config-row-grid" style={{ gridTemplateColumns: rowTemplate, columnGap: rowGap, width: rowWidth }}>
                <div className="admin-config-field">
                  <span className="admin-config-field-label">key</span>
                  {row.isEditing ? (
                    <input
                      className="text-field admin-config-input"
                      value={row.key}
                      onChange={(event) => handleRowChange(row.originalKey, row.originalLanguage, 'key', event.target.value)}
                      disabled={row.isSaving}
                    />
                  ) : (
                    <div className="admin-config-display">{row.key}</div>
                  )}
                  {renderResizer(0, 'key')}
                </div>

                <div className="admin-config-field">
                  <span className="admin-config-field-label">value</span>
                  {row.isEditing ? (
                    <textarea
                      className="text-field admin-config-textarea"
                      rows={1}
                      value={row.value}
                      onChange={(event) => {
                        resizeTextareaHeight(event.currentTarget);
                        handleRowChange(row.originalKey, row.originalLanguage, 'value', event.currentTarget.value);
                      }}
                      disabled={row.isSaving}
                    />
                  ) : (
                    <div className="admin-config-display admin-config-display-multiline">{row.value}</div>
                  )}
                  {renderResizer(1, 'value')}
                </div>

                <div className="admin-config-field">
                  <span className="admin-config-field-label">language</span>
                  {row.isEditing ? (
                    <input
                      className="text-field admin-config-input"
                      value={row.language}
                      onChange={(event) => handleRowChange(row.originalKey, row.originalLanguage, 'language', event.target.value)}
                      disabled={row.isSaving}
                    />
                  ) : (
                    <div className="admin-config-display">{row.language}</div>
                  )}
                  {renderResizer(2, 'language')}
                </div>

                <div className="admin-config-field">
                  <span className="admin-config-field-label">description</span>
                  {row.isEditing ? (
                    <input
                      className="text-field admin-config-input"
                      value={row.description}
                      onChange={(event) =>
                        handleRowChange(row.originalKey, row.originalLanguage, 'description', event.target.value)
                      }
                      disabled={row.isSaving}
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
                        onClick={() => void handleUpdateUiText(row.originalKey, row.originalLanguage)}
                        disabled={row.isSaving}
                        aria-label="저장"
                        title="저장"
                      >
                        <CheckIcon />
                      </button>
                      <button
                        type="button"
                        className="btn text admin-config-icon-button"
                        onClick={() => handleCancelEditing(row.originalKey, row.originalLanguage)}
                        disabled={row.isSaving}
                        aria-label="취소"
                        title="취소"
                      >
                        <CloseIcon />
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        type="button"
                        className="btn text admin-config-icon-button"
                        onClick={() => handleStartEditing(row.originalKey, row.originalLanguage)}
                        disabled={row.isSaving}
                        aria-label="수정"
                        title="수정"
                      >
                        <EditIcon />
                      </button>
                      {isDeletableLanguage(row.originalLanguage) ? (
                        <button
                          type="button"
                          className="btn text admin-config-icon-button admin-config-delete-button"
                          onClick={() => void handleDeleteUiText(row.originalKey, row.originalLanguage)}
                          disabled={row.isSaving}
                          aria-label="삭제"
                          title="삭제"
                        >
                          <DeleteIcon />
                        </button>
                      ) : null}
                    </>
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
