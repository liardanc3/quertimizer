import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import HttpErrorState from '../components/common/HttpErrorState';
import { LoadingOverlay } from '../components/common/LoadingSpinner';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { getApiErrorStatus, isCommonHttpErrorStatus } from '../lib/apiError';
import { createUiText, deleteUiText, fetchAdminUiTexts, getUiTextValue, updateUiText, useUiText, type UiTextData } from '../lib/uiText';
import { defaultUiTextMap } from '../lib/defaultUiTexts';

interface EditableUiTextRow extends UiTextData {
  rowId: string;
  originalKey: string;
  originalLanguage: string;
  originalValue: string;
  originalDescription: string;
  isNew: boolean;
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

const ADMIN_CONFIG_PAGE_SIZE = 10;
const ADMIN_CONFIG_COLUMN_WEIGHTS = [4, 4, 1, 2];
const ADMIN_CONFIG_MINIMUM_WEIGHTS = [2.2, 2.2, 0.72, 1.3];
const ADMIN_CONFIG_ACTIONS_WIDTH = 68;
const ADMIN_CONFIG_COLUMN_GAP = 6;
const NOTIFICATION_UI_TEXT_KEY = 'NOTIFICATION';
const ADMIN_CONFIG_AVAILABLE_KEYS = Object.keys(defaultUiTextMap).sort((left, right) => left.localeCompare(right));

function toEditableRow(uiText: UiTextData): EditableUiTextRow {
  return {
    ...uiText,
    rowId: `${uiText.key}:${uiText.language}`,
    originalKey: uiText.key,
    originalLanguage: uiText.language,
    originalValue: uiText.value,
    originalDescription: uiText.description,
    isNew: false,
    isEditing: false,
    isSaving: false,
    errorMessage: null,
  };
}

function createEditableRowId() {
  return `new:${Date.now()}:${Math.random().toString(16).slice(2)}`;
}

function createNewRow(existingKeys: string[]): EditableUiTextRow {
  return {
    rowId: createEditableRowId(),
    key: existingKeys[0] ?? '',
    value: '',
    language: '',
    description: '',
    originalKey: '',
    originalLanguage: '',
    originalValue: '',
    originalDescription: '',
    isNew: true,
    isEditing: true,
    isSaving: false,
    errorMessage: null,
  };
}

function sortUiTextRows(rows: EditableUiTextRow[]) {
  function getNotificationPriority(row: EditableUiTextRow) {
    return row.key === NOTIFICATION_UI_TEXT_KEY ? 0 : 1;
  }

  return [...rows].sort(
    (left, right) =>
      Number(right.isNew) - Number(left.isNew) ||
      getNotificationPriority(left) - getNotificationPriority(right) ||
      left.key.localeCompare(right.key) ||
      left.language.localeCompare(right.language) ||
      left.value.localeCompare(right.value),
  );
}

function isSameRow(row: EditableUiTextRow, rowId: string) {
  return row.rowId === rowId;
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

function PlusIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="M10 4.75v10.5M4.75 10h10.5"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
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
  const { text } = useUiText();
  const [uiTextRows, setUiTextRows] = useState<EditableUiTextRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadErrorMessage, setLoadErrorMessage] = useState<string | null>(null);
  const [loadErrorStatus, setLoadErrorStatus] = useState<number | null>(null);
  const [columnWidths, setColumnWidths] = useState<number[]>([]);
  const [resizeState, setResizeState] = useState<ColumnResizeState | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [searchDraft, setSearchDraft] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [isPageJumpEditing, setIsPageJumpEditing] = useState(false);
  const [pageJumpDraft, setPageJumpDraft] = useState('1');
  const loadSequenceRef = useRef(0);
  const keyLabel = text('COMMON_KEY_LABEL', 'key');
  const valueLabel = text('COMMON_VALUE_LABEL', 'value');
  const languageLabel = text('COMMON_LANGUAGE_LABEL', 'language');
  const descriptionLabel = text('COMMON_DESCRIPTION_LABEL', 'description');
  const hasActiveSearchQuery = searchQuery.trim() !== '';
  const availableKeys = useMemo(() => ADMIN_CONFIG_AVAILABLE_KEYS, []);

  async function loadUiTexts(page: number, query: string) {
    const loadSequence = loadSequenceRef.current + 1;
    loadSequenceRef.current = loadSequence;
    setIsLoading(true);
    setLoadErrorMessage(null);
    setLoadErrorStatus(null);

    try {
      const loadedPage = await fetchAdminUiTexts({
        page,
        pageSize: ADMIN_CONFIG_PAGE_SIZE,
        query,
      });

      if (loadSequence !== loadSequenceRef.current) {
        return;
      }

      setUiTextRows(sortUiTextRows(loadedPage.uiTexts.map(toEditableRow)));
      setTotalPages(loadedPage.totalPages);

      if (loadedPage.currentPage !== page) {
        setCurrentPage(loadedPage.currentPage);
      }

      if (!isPageJumpEditing) {
        setPageJumpDraft(String(loadedPage.currentPage));
      }
    } catch (error) {
      if (loadSequence !== loadSequenceRef.current) {
        return;
      }

      setLoadErrorMessage(error instanceof Error ? error.message : getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
      const status = getApiErrorStatus(error);
      setLoadErrorStatus(isCommonHttpErrorStatus(status) ? status : null);
    } finally {
      if (loadSequence === loadSequenceRef.current) {
        setIsLoading(false);
      }
    }
  }

  useEffect(() => {
    void loadUiTexts(currentPage, searchQuery);
  }, [currentPage, searchQuery]);

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

  function handleRowChange(rowId: string, field: UiTextField, value: string) {
    setUiTextRows((currentRows) =>
      currentRows.map((row) =>
        isSameRow(row, rowId)
          ? {
              ...row,
              [field]: value,
              errorMessage: null,
            }
          : row,
      ),
    );
  }

  function handleAddUiText() {
    setUiTextRows((currentRows) => {
      if (currentRows.some((row) => row.isNew)) {
        return currentRows;
      }

      if (availableKeys.length === 0) {
        return currentRows;
      }

      return sortUiTextRows([createNewRow(availableKeys), ...currentRows]);
    });
  }

  function handleStartEditing(rowId: string) {
    setUiTextRows((currentRows) =>
      currentRows.map((row) =>
        isSameRow(row, rowId)
          ? {
              ...row,
              isEditing: true,
              errorMessage: null,
            }
          : row,
      ),
    );
  }

  function handleCancelEditing(rowId: string) {
    setUiTextRows((currentRows) => {
      const targetRow = currentRows.find((row) => isSameRow(row, rowId));
      if (!targetRow) {
        return currentRows;
      }

      if (targetRow.isNew) {
        return currentRows.filter((row) => !isSameRow(row, rowId));
      }

      return currentRows.map((row) =>
        isSameRow(row, rowId)
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
      );
    });
  }

  async function handleSaveUiText(rowId: string) {
    const targetRow = uiTextRows.find((row) => isSameRow(row, rowId));
    if (!targetRow) {
      return;
    }

    setUiTextRows((currentRows) =>
      currentRows.map((row) =>
        isSameRow(row, rowId)
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
      if (targetRow.isNew) {
        await createUiText({
            key: targetRow.key,
            value: targetRow.value,
            language: targetRow.language,
            description: targetRow.description,
          });
      } else {
        await updateUiText(targetRow.originalKey, targetRow.originalLanguage, {
            key: targetRow.key,
            value: targetRow.value,
            language: targetRow.language,
            description: targetRow.description,
          });
      }

      await loadUiTexts(currentPage, searchQuery);
    } catch (error) {
      setUiTextRows((currentRows) =>
        currentRows.map((row) =>
          isSameRow(row, rowId)
            ? {
                ...row,
                isEditing: true,
                isSaving: false,
                errorMessage:
                  error instanceof Error
                    ? error.message
                    : getUiTextValue(
                        targetRow.isNew ? 'GLOBAL_CONFIG_CREATE_FAIL_MESSAGE' : 'GLOBAL_CONFIG_UPDATE_FAIL_MESSAGE',
                        targetRow.isNew ? 'UI 텍스트를 생성하지 못했습니다.' : 'UI 텍스트를 수정하지 못했습니다.',
                      ),
              }
            : row,
        ),
      );
    }
  }

  async function handleDeleteUiText(rowId: string) {
    const targetRow = uiTextRows.find((row) => isSameRow(row, rowId));
    if (!targetRow) {
      return;
    }

    setUiTextRows((currentRows) =>
      currentRows.map((row) =>
        isSameRow(row, rowId)
          ? {
              ...row,
              isSaving: true,
              errorMessage: null,
            }
          : row,
      ),
    );

    try {
      await deleteUiText(targetRow.originalKey, targetRow.originalLanguage);
      await loadUiTexts(currentPage, searchQuery);
    } catch (error) {
      setUiTextRows((currentRows) =>
        currentRows.map((row) =>
          isSameRow(row, rowId)
            ? {
                ...row,
                isSaving: false,
                errorMessage: error instanceof Error ? error.message : getUiTextValue('GLOBAL_CONFIG_DELETE_FAIL_MESSAGE', 'UI 텍스트를 삭제하지 못했습니다.'),
              }
            : row,
        ),
      );
    }
  }

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextQuery = searchDraft.trim();

    setSearchQuery(nextQuery);
    setCurrentPage(1);
    setIsPageJumpEditing(false);
    setPageJumpDraft('1');
  }

  function handleResetSearch() {
    setSearchDraft('');
    setSearchQuery('');
    setCurrentPage(1);
    setIsPageJumpEditing(false);
    setPageJumpDraft('1');
  }

  function applyPageJump() {
    const parsedPage = Number.parseInt(pageJumpDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? currentPage
      : Math.min(totalPages, Math.max(1, parsedPage));

    setPageJumpDraft(String(nextPage));
    setIsPageJumpEditing(false);

    if (nextPage !== currentPage) {
      setCurrentPage(nextPage);
    }
  }

  function cancelPageJump() {
    setPageJumpDraft(String(currentPage));
    setIsPageJumpEditing(false);
  }

  const resolvedColumnWidths =
    columnWidths.length === ADMIN_CONFIG_COLUMN_WEIGHTS.length
      ? columnWidths
      : resolveColumnWidths(0, ADMIN_CONFIG_COLUMN_WEIGHTS);
  const hasPendingNewRow = uiTextRows.some((row) => row.isNew);
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
        aria-label={text('GLOBAL_CONFIG_COLUMN_RESIZE_LABEL', { label }, '{label} 너비 조절')}
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
    <section className="admin-config-panel is-global-config-panel">
      <form className="admin-config-toolbar admin-config-toolbar-form admin-config-search-form" onSubmit={handleSearchSubmit}>
        <label className="problem-search-field admin-config-search-field">
          <input
            type="search"
            className="text-field problem-search-input home-problem-search-input submit-history-search-input admin-config-search-input"
            value={searchDraft}
            onChange={(event) => setSearchDraft(event.target.value)}
            placeholder={text('GLOBAL_CONFIG_SEARCH_PLACEHOLDER', 'key, value, description 검색')}
            aria-label={text('GLOBAL_CONFIG_SEARCH_LABEL', 'UI 텍스트 검색')}
          />
          <button
            type="submit"
            className="btn secondary problem-search-button admin-config-search-button"
            aria-label={text('COMMON_SEARCH_BUTTON', '검색')}
          />
        </label>

        <div className="admin-config-toolbar-actions">
          {hasActiveSearchQuery || searchDraft.trim() !== '' ? (
            <button type="button" className="btn text admin-config-toolbar-button" onClick={handleResetSearch}>
              {text('COMMON_CANCEL_BUTTON', '취소')}
            </button>
          ) : null}
          <button
            type="button"
            className="btn text admin-config-icon-button admin-config-add-button"
            onClick={handleAddUiText}
            disabled={isLoading || hasPendingNewRow || availableKeys.length === 0}
            aria-label={text('COMMON_ADD_BUTTON', '추가')}
            title={text('COMMON_ADD_BUTTON', '추가')}
          >
            <PlusIcon />
          </button>
        </div>
      </form>

      {loadErrorMessage && !isLoading
        ? loadErrorStatus != null
          ? <HttpErrorState status={loadErrorStatus} className="admin-config-empty" message={loadErrorMessage} />
          : <PageLoadFailureState className="admin-config-empty" message={loadErrorMessage} />
        : null}

      {isLoading ? (
        <div
          className="admin-page-loading-shell admin-config-loading-shell is-loading"
          aria-live="polite"
          aria-label={text('COMMON_LOADING_STATUS', '로딩 중')}
        >
          <div className="admin-page-loading-body" aria-hidden="true">
            <div className="admin-page-loading-row is-wide" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row is-narrow" />
          </div>

          <LoadingOverlay ariaHidden />
        </div>
      ) : loadErrorMessage ? null : uiTextRows.length === 0 ? (
        <div className="admin-config-empty">
          {hasActiveSearchQuery
            ? text('GLOBAL_CONFIG_SEARCH_EMPTY_STATE', '검색 결과가 없습니다.')
            : text('GLOBAL_CONFIG_EMPTY_STATE', '등록된 UI 텍스트가 없습니다.')}
        </div>
      ) : (
        <>
          <div className="admin-config-table" ref={gridRef} role="table" aria-label={text('GLOBAL_CONFIG_TABLE_LABEL', 'UI 텍스트 목록')}>
            <div className="admin-config-header" role="row" style={{ gridTemplateColumns: rowTemplate, columnGap: rowGap, width: rowWidth }}>
              <div className="admin-config-header-cell">
                <span>{keyLabel}</span>
                {renderResizer(0, keyLabel)}
              </div>
              <div className="admin-config-header-cell">
                <span>{valueLabel}</span>
                {renderResizer(1, valueLabel)}
              </div>
              <div className="admin-config-header-cell">
                <span>{languageLabel}</span>
                {renderResizer(2, languageLabel)}
              </div>
              <div className="admin-config-header-cell">
                <span>{descriptionLabel}</span>
              </div>
              <div className="admin-config-header-cell admin-config-header-cell-actions" aria-hidden="true" />
            </div>

            {uiTextRows.map((row) => (
              <div key={row.rowId} className="admin-config-row" role="row">
                <div className="admin-config-row-grid" style={{ gridTemplateColumns: rowTemplate, columnGap: rowGap, width: rowWidth }}>
                  <div className="admin-config-field">
                    <span className="admin-config-field-label">{keyLabel}</span>
                    {row.isNew ? (
                      <select
                        className="text-field admin-config-input admin-config-select"
                        value={row.key}
                        onChange={(event) => handleRowChange(row.rowId, 'key', event.target.value)}
                        disabled={row.isSaving}
                      >
                        {availableKeys.map((keyOption) => (
                          <option key={keyOption} value={keyOption}>
                            {keyOption}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <div className="admin-config-display">{row.key}</div>
                    )}
                    {renderResizer(0, keyLabel)}
                  </div>

                  <div className="admin-config-field">
                    <span className="admin-config-field-label">{valueLabel}</span>
                    {row.isEditing ? (
                      <textarea
                        className="text-field admin-config-textarea"
                        rows={1}
                        value={row.value}
                        onChange={(event) => {
                          resizeTextareaHeight(event.currentTarget);
                          handleRowChange(row.rowId, 'value', event.currentTarget.value);
                        }}
                        disabled={row.isSaving}
                      />
                    ) : (
                      <div className="admin-config-display admin-config-display-multiline">{row.value}</div>
                    )}
                    {renderResizer(1, valueLabel)}
                  </div>

                  <div className="admin-config-field">
                    <span className="admin-config-field-label">{languageLabel}</span>
                    {row.isEditing ? (
                      <input
                        className="text-field admin-config-input"
                        value={row.language}
                        onChange={(event) => handleRowChange(row.rowId, 'language', event.target.value)}
                        disabled={row.isSaving}
                      />
                    ) : (
                      <div className="admin-config-display">{row.language}</div>
                    )}
                    {renderResizer(2, languageLabel)}
                  </div>

                  <div className="admin-config-field">
                    <span className="admin-config-field-label">{descriptionLabel}</span>
                    {row.isEditing ? (
                      <input
                        className="text-field admin-config-input"
                        value={row.description}
                        onChange={(event) => handleRowChange(row.rowId, 'description', event.target.value)}
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
                          onClick={() => void handleSaveUiText(row.rowId)}
                          disabled={row.isSaving}
                          aria-label={text('COMMON_SAVE_BUTTON', '저장')}
                          title={text('COMMON_SAVE_BUTTON', '저장')}
                        >
                          <CheckIcon />
                        </button>
                        <button
                          type="button"
                          className="btn text admin-config-icon-button"
                          onClick={() => handleCancelEditing(row.rowId)}
                          disabled={row.isSaving}
                          aria-label={text('COMMON_CANCEL_BUTTON', '취소')}
                          title={text('COMMON_CANCEL_BUTTON', '취소')}
                        >
                          <CloseIcon />
                        </button>
                      </>
                    ) : (
                      <>
                        <button
                          type="button"
                          className="btn text admin-config-icon-button"
                          onClick={() => handleStartEditing(row.rowId)}
                          disabled={row.isSaving}
                          aria-label={text('COMMON_EDIT_BUTTON', '수정')}
                          title={text('COMMON_EDIT_BUTTON', '수정')}
                        >
                          <EditIcon />
                        </button>
                        {isDeletableLanguage(row.originalLanguage) ? (
                          <button
                            type="button"
                            className="btn text admin-config-icon-button admin-config-delete-button"
                            onClick={() => void handleDeleteUiText(row.rowId)}
                            disabled={row.isSaving}
                            aria-label={text('COMMON_DELETE_BUTTON', '삭제')}
                            title={text('COMMON_DELETE_BUTTON', '삭제')}
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

          <div className="admin-config-pagination" role="navigation" aria-label={text('GLOBAL_CONFIG_PAGE_LABEL', 'UI 텍스트 페이지')}>
            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
              disabled={currentPage <= 1}
            >
              {text('COMMON_PREVIOUS_BUTTON', '이전')}
            </button>

            {isPageJumpEditing ? (
              <input
                type="text"
                inputMode="numeric"
                className="problem-pagination-meta-input admin-config-pagination-input"
                value={pageJumpDraft}
                onChange={(event) => setPageJumpDraft(event.target.value.replace(/\D+/g, ''))}
                onBlur={applyPageJump}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    applyPageJump();
                    return;
                  }

                  if (event.key === 'Escape') {
                    event.preventDefault();
                    cancelPageJump();
                  }
                }}
                aria-label={text('GLOBAL_CONFIG_PAGE_INPUT_LABEL', '이동할 UI 텍스트 페이지 입력')}
                autoFocus
              />
            ) : (
              <button
                type="button"
                className="problem-pagination-meta problem-pagination-meta-button admin-config-pagination-meta"
                aria-label={text('GLOBAL_CONFIG_PAGE_INPUT_OPEN_LABEL', '이동할 UI 텍스트 페이지 입력 열기')}
                onClick={() => {
                  setPageJumpDraft(String(currentPage));
                  setIsPageJumpEditing(true);
                }}
              >
                {`${currentPage} / ${totalPages}`}
              </button>
            )}

            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
              disabled={currentPage >= totalPages}
            >
              {text('COMMON_NEXT_BUTTON', '다음')}
            </button>
          </div>
        </>
      )}
    </section>
  );
}
