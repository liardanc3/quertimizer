import { useEffect, useMemo, useRef, useState, type Dispatch, type ReactNode, type SetStateAction } from 'react';
import StatusPopup from '../components/common/StatusPopup';
import useDismissableLayer from '../hooks/useDismissableLayer';
import {
  createProblem,
  fetchAdminProblemOptions,
  fetchProblemDetail,
  fetchProblemSetDetail,
  fetchProblemSets,
  previewProblemOutput,
  type DbmsType,
  type ProblemDetailData,
  type ProblemOutputPreviewData,
  type ProblemSetDetailData,
  type ProblemSetSummary,
} from '../lib/problemApi';
import { navigate } from '../lib/navigation';
import { getUiText, getUiTextValue, useUiText } from '../lib/uiText';
import './ProblemCreatePage.css';

type SectionKey = 'condition' | 'output' | 'ddl' | 'actualData' | 'sampleData' | 'outputPreview' | 'answerSql';
type MissingFieldKey = 'title' | 'description' | 'condition' | 'output' | 'ddl' | 'actualData' | 'sampleData' | 'answerSql';

interface EditableDraftState<T> {
  appliedValue: T;
  draftValue: T;
  isEditing: boolean;
  setDraftValue: Dispatch<SetStateAction<T>>;
  startEditing: () => void;
  cancelEditing: () => void;
  confirmEditing: () => void;
  replaceValue: (nextValue: T) => void;
}

interface SourceDraft {
  postgresql: string;
  mysql: string;
}

interface PopupState {
  open: boolean;
  level: 1 | 2 | 3;
  message: string;
}

interface ProblemCreateSelectOption {
  value: string;
  label: string;
}

interface MissingField {
  key: MissingFieldKey;
  label: string;
}

interface SectionRefs {
  condition: HTMLElement | null;
  output: HTMLElement | null;
  ddl: HTMLElement | null;
  actualData: HTMLElement | null;
  sampleData: HTMLElement | null;
  answerSql: HTMLElement | null;
}

const EMPTY_PROBLEM_SET_DETAIL: ProblemSetDetailData = {
  problemSetId: '',
  ddlPostgresql: '',
  ddlMysql: '',
  dataPostgresql: '',
  dataMysql: '',
};

const EMPTY_PREVIEW_OUTPUT: ProblemOutputPreviewData = {
  columns: [],
  rows: [],
  rowCount: 0,
};

const NEW_PROBLEM_SET_OPTION_VALUE = '__new__';
const NEW_PROBLEM_OPTION_VALUE = '__new_problem__';

function useEditableDraft<T>(initialValue: T): EditableDraftState<T> {
  const [appliedValue, setAppliedValue] = useState(initialValue);
  const [draftValue, setDraftValue] = useState(initialValue);
  const [isEditing, setIsEditing] = useState(false);

  function startEditing() {
    setDraftValue(appliedValue);
    setIsEditing(true);
  }

  function cancelEditing() {
    setDraftValue(appliedValue);
    setIsEditing(false);
  }

  function confirmEditing() {
    setAppliedValue(draftValue);
    setIsEditing(false);
  }

  function replaceValue(nextValue: T) {
    setAppliedValue(nextValue);
    setDraftValue(nextValue);
    setIsEditing(false);
  }

  return {
    appliedValue,
    draftValue,
    isEditing,
    setDraftValue,
    startEditing,
    cancelEditing,
    confirmEditing,
    replaceValue,
  };
}

function getProblemSetLabel(problemSetId: string) {
  return getUiText('PROBLEM_CREATE_SET_LABEL', { problemSetId }, `테이블셋 ${problemSetId}`);
}

function getProblemLabel(problemId: string) {
  return getUiText('PROBLEM_CREATE_PROBLEM_LABEL', { problemId }, `문제 ${problemId}`);
}

function getProblemNumberLabel(existingProblemSet: boolean, existingProblem: boolean, problemSetId: string | null, problemId: string | null) {
  if (existingProblemSet && existingProblem && problemId) {
    return problemId;
  }

  if (existingProblemSet && problemSetId) {
    return getUiText('PROBLEM_CREATE_NEW_SET_NUMBER_LABEL', { problemSetId }, `${problemSetId}-신규`);
  }

  return getUiTextValue('PROBLEM_CREATE_NEW_PROBLEM_LABEL', '신규 문제');
}

function resolveScopedDbms(value: string | null | undefined): DbmsType {
  return value?.trim().startsWith('M') ? 'mysql' : 'postgresql';
}

function getTableNamesFromDdl(ddl: string) {
  const pattern = /CREATE TABLE\s+(?:[\w]+\.)?(\w+)\s*\(/gi;
  const tableNames: string[] = [];
  let match: RegExpExecArray | null;

  while ((match = pattern.exec(ddl)) != null) {
    if (!tableNames.includes(match[1])) {
      tableNames.push(match[1]);
    }
  }

  return tableNames;
}

function filterDdlByTableNames(ddl: string, includedTableNames: string[]) {
  if (includedTableNames.length === 0 || ddl.trim() === '') {
    return ddl.trim();
  }

  const tableNameSet = new Set(includedTableNames);
  const createTablePattern = /CREATE TABLE\s+(?:[\w]+\.)?(\w+)\s*\([\s\S]*?\)\s*(?:ENGINE\s*=\s*\w+\s*)?(?:COMMENT\s*=\s*'[^']*'\s*)?;/gi;
  const commentPattern = /COMMENT ON (TABLE|COLUMN)\s+([\s\S]*?);/gi;
  const fragments: string[] = [];
  let match: RegExpExecArray | null;

  while ((match = createTablePattern.exec(ddl)) != null) {
    if (tableNameSet.has(match[1])) {
      fragments.push(match[0].trim());
    }
  }

  while ((match = commentPattern.exec(ddl)) != null) {
    const targetTableName = match[2].trim().split('.')[0]?.trim().replace(/^.*\./, '');
    if (targetTableName && tableNameSet.has(targetTableName)) {
      fragments.push(`COMMENT ON ${match[1]} ${match[2]};`.trim());
    }
  }

  return fragments.join('\n\n').trim();
}

function filterDataSqlByTableNames(dataSql: string, includedTableNames: string[]) {
  if (includedTableNames.length === 0 || dataSql.trim() === '') {
    return dataSql.trim();
  }

  const tableNameSet = new Set(includedTableNames);
  const insertPattern = /INSERT INTO\s+(?:[\w]+\.)?(\w+)\s*\(([\s\S]*?)\)\s*VALUES\s*([\s\S]*?);/gi;
  const fragments: string[] = [];
  let match: RegExpExecArray | null;

  while ((match = insertPattern.exec(dataSql)) != null) {
    if (tableNameSet.has(match[1])) {
      fragments.push(match[0].trim());
    }
  }

  return fragments.join('\n\n').trim();
}

function arraysEqual(left: string[], right: string[]) {
  if (left.length !== right.length) {
    return false;
  }

  return left.every((value, index) => value === right[index]);
}

function parseCsvLine(line: string) {
  const cells: string[] = [];
  let currentValue = '';
  let inQuote = false;

  for (let index = 0; index < line.length; index += 1) {
    const currentCharacter = line[index];
    const nextCharacter = line[index + 1];

    if (currentCharacter === '"') {
      if (inQuote && nextCharacter === '"') {
        currentValue += '"';
        index += 1;
        continue;
      }

      inQuote = !inQuote;
      continue;
    }

    if (!inQuote && currentCharacter === ',') {
      cells.push(currentValue.trim());
      currentValue = '';
      continue;
    }

    currentValue += currentCharacter;
  }

  cells.push(currentValue.trim());
  return cells;
}

function parseCsvValue(value: string): string | number | boolean | null {
  if (/^null$/i.test(value)) return null;
  if (/^true$/i.test(value)) return true;
  if (/^false$/i.test(value)) return false;
  if (/^-?\d+(?:\.\d+)?$/.test(value)) return Number(value);
  return value;
}

function parseProblemOutputSample(rawSampleOutput: string): ProblemOutputPreviewData {
  if (rawSampleOutput.trim() === '') {
    return EMPTY_PREVIEW_OUTPUT;
  }

  try {
    const parsed = JSON.parse(rawSampleOutput) as { columns?: unknown; rows?: unknown; rowCount?: unknown };
    if (Array.isArray(parsed.columns) && Array.isArray(parsed.rows)) {
      const rows = parsed.rows.map((row) =>
        Array.isArray(row)
          ? row.map((value) =>
              typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' || value == null
                ? value
                : String(value),
            )
          : [],
      );

      return {
        columns: parsed.columns.filter((column): column is string => typeof column === 'string'),
        rows,
        rowCount: typeof parsed.rowCount === 'number' ? parsed.rowCount : rows.length,
      };
    }
  } catch {
  }

  try {
    const lines = rawSampleOutput
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean);

    if (lines.length === 0) {
      return EMPTY_PREVIEW_OUTPUT;
    }

    const [headerLine, ...dataLines] = lines;
    const rows = dataLines.map((line) => parseCsvLine(line).map(parseCsvValue));

    return {
      columns: parseCsvLine(headerLine),
      rows,
      rowCount: rows.length,
    };
  } catch {
    return EMPTY_PREVIEW_OUTPUT;
  }
}

function buildMissingFields(values: {
  title: string;
  description: string;
  condition: string;
  output: string;
  ddl: string;
  actualData: string;
  sampleData: string;
  answerSql: string;
}) {
  const missingFields: MissingField[] = [];

  if (values.title.trim() === '') missingFields.push({ key: 'title', label: getUiTextValue('PROBLEM_CREATE_TITLE_LABEL', '문제 제목') });
  if (values.description.trim() === '') missingFields.push({ key: 'description', label: getUiTextValue('PROBLEM_CREATE_DESCRIPTION_LABEL', '설명') });
  if (values.condition.trim() === '') missingFields.push({ key: 'condition', label: getUiTextValue('PROBLEM_CREATE_CONDITION_LABEL', '조건') });
  if (values.output.trim() === '') missingFields.push({ key: 'output', label: getUiTextValue('PROBLEM_CREATE_OUTPUT_LABEL', '출력 설명') });
  if (values.ddl.trim() === '') missingFields.push({ key: 'ddl', label: getUiTextValue('PROBLEM_CREATE_DDL_LABEL', '테이블 정보 DDL') });
  if (values.actualData.trim() === '') missingFields.push({ key: 'actualData', label: getUiTextValue('PROBLEM_CREATE_ACTUAL_DATA_LABEL', '실제 채점 데이터') });
  if (values.sampleData.trim() === '') missingFields.push({ key: 'sampleData', label: getUiTextValue('PROBLEM_CREATE_SAMPLE_DATA_LABEL', '예시 데이터') });
  if (values.answerSql.trim() === '') missingFields.push({ key: 'answerSql', label: getUiTextValue('PROBLEM_CREATE_ANSWER_SQL_LABEL', '정답 SQL') });

  return missingFields;
}

function EditIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path fill="currentColor" d="M3 17.25V21h3.75l11-11.03-3.75-3.75L3 17.25Zm14.71-9.04a1.003 1.003 0 0 0 0-1.42l-2.5-2.5a1.003 1.003 0 0 0-1.42 0l-1.96 1.96 3.75 3.75 2.13-1.79Z" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path fill="currentColor" d="M18.3 5.71 12 12l6.3 6.29-1.41 1.41L10.59 13.4 4.29 19.7 2.88 18.29 9.17 12 2.88 5.71 4.29 4.29l6.3 6.3 6.29-6.3 1.42 1.42Z" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path fill="currentColor" d="m9 16.17-3.88-3.88L3.71 13.7 9 19l12-12-1.41-1.41z" />
    </svg>
  );
}

function ChevronIcon({ collapsed }: { collapsed: boolean }) {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d={collapsed ? 'm6 7.2 4 4 4-4' : 'm7.2 6 4 4-4 4'}
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  );
}

function SelectChevronIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="m5.5 7.8 4.5 4.4 4.5-4.4" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
    </svg>
  );
}

function InlineEditActions({ isEditing, onEdit, onCancel, onConfirm }: { isEditing: boolean; onEdit: () => void; onCancel: () => void; onConfirm: () => void }) {
  return (
    <div className="problem-create-inline-toolbar">
      <button
        type="button"
        className={`problem-create-inline-action ${isEditing ? 'is-active' : ''}`.trim()}
        aria-label={isEditing ? getUiTextValue('COMMON_CANCEL_BUTTON', '취소') : getUiTextValue('COMMON_EDIT_BUTTON', '수정')}
        onClick={isEditing ? onCancel : onEdit}
      >
        {isEditing ? <CloseIcon /> : <EditIcon />}
      </button>
      {isEditing ? (
        <button type="button" className="problem-create-inline-action problem-create-confirm-action" aria-label={getUiTextValue('COMMON_APPLY_BUTTON', '적용')} onClick={onConfirm}>
          <CheckIcon />
        </button>
      ) : null}
    </div>
  );
}

function ProblemCreateSelectMenu({
  value,
  options,
  onChange,
  className,
  disabled = false,
}: {
  value: string;
  options: ProblemCreateSelectOption[];
  onChange: (value: string) => void;
  className?: string;
  disabled?: boolean;
}) {
  const shellRef = useRef<HTMLDivElement | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const dismissLayerRefs = useMemo(() => [shellRef], []);
  const selectedOption = options.find((option) => option.value === value) ?? options[0];

  useDismissableLayer({
    enabled: isOpen,
    refs: dismissLayerRefs,
    onDismiss: () => setIsOpen(false),
  });

  return (
    <div ref={shellRef} className={`problem-create-select-shell ${isOpen ? 'is-open' : ''} ${disabled ? 'is-disabled' : ''}`.trim()}>
      <button
        type="button"
        className={`text-field problem-create-select-trigger ${className ?? ''}`.trim()}
        onClick={() => {
          if (!disabled) {
            setIsOpen((current) => !current);
          }
        }}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        disabled={disabled}
      >
        <span className="problem-create-select-trigger-text">{selectedOption?.label ?? ''}</span>
        <span className="problem-create-select-trigger-icon" aria-hidden="true">
          <SelectChevronIcon />
        </span>
      </button>

      {isOpen ? (
        <div className="problem-create-select-menu" role="listbox">
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              role="option"
              aria-selected={option.value === value}
              className={`problem-create-select-option ${option.value === value ? 'is-selected' : ''}`.trim()}
              onClick={() => {
                onChange(option.value);
                setIsOpen(false);
              }}
            >
              {option.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}

function ProblemCreateSection({
  sectionKey,
  title,
  collapsed,
  onToggle,
  isMissing,
  actions,
  children,
  sectionRef,
}: {
  sectionKey: SectionKey;
  title: string;
  collapsed: boolean;
  onToggle: (sectionKey: SectionKey) => void;
  isMissing?: boolean;
  actions?: ReactNode;
  children: ReactNode;
  sectionRef?: (node: HTMLElement | null) => void;
}) {
  return (
    <section ref={sectionRef} className={`problem-create-section ${collapsed ? 'is-collapsed' : ''} ${isMissing ? 'is-missing' : ''}`.trim()}>
      <div className="problem-create-section-header">
        <button
          type="button"
          className="problem-create-section-toggle"
          aria-label={collapsed ? getUiTextValue('COMMON_EXPAND_ACTION', '펼치기') : getUiTextValue('COMMON_COLLAPSE_ACTION', '접기')}
          aria-expanded={!collapsed}
          onClick={() => onToggle(sectionKey)}
        >
          <ChevronIcon collapsed={collapsed} />
        </button>
        <h2 className="problem-create-section-title">{title}</h2>
        <div className="problem-create-section-actions">{actions}</div>
      </div>

      {!collapsed ? <div className="problem-create-section-body">{children}</div> : null}
    </section>
  );
}

function ProblemCreatePreviewGrid({ previewData }: { previewData: ProblemOutputPreviewData }) {
  if (previewData.columns.length === 0) {
    return <p className="problem-create-empty">{getUiTextValue('PROBLEM_CREATE_OUTPUT_EMPTY_STATE', '출력 예시가 아직 없습니다.')}</p>;
  }

  return (
    <div className="problem-create-preview-grid-shell">
      <div className="problem-create-preview-meta">{getUiText('PROBLEM_CREATE_PREVIEW_ROW_COUNT_LABEL', { count: previewData.rowCount }, `행 ${previewData.rowCount}개`)}</div>
      <div className="problem-create-preview-grid">
        <table>
          <thead>
            <tr>
              {previewData.columns.map((column) => (
                <th key={column}>{column}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {previewData.rows.map((row, rowIndex) => (
              <tr key={`preview-row-${rowIndex}`}>
                {row.map((value, columnIndex) => (
                  <td key={`preview-cell-${rowIndex}-${columnIndex}`}>{value == null ? '' : String(value)}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export function ProblemCreateContent() {
  const { text } = useUiText();
  const pageRef = useRef<HTMLDivElement | null>(null);
  const heroSectionRef = useRef<HTMLElement | null>(null);
  const sectionRefs = useRef<SectionRefs>({
    condition: null,
    output: null,
    ddl: null,
    actualData: null,
    sampleData: null,
    answerSql: null,
  });

  const heroState = useEditableDraft({ title: '', description: '' });
  const conditionState = useEditableDraft('');
  const outputState = useEditableDraft('');
  const ddlState = useEditableDraft<SourceDraft>({ postgresql: '', mysql: '' });
  const actualDataState = useEditableDraft<SourceDraft>({ postgresql: '', mysql: '' });
  const sampleDataState = useEditableDraft('');
  const answerSqlState = useEditableDraft('');

  const [existingProblemSet, setExistingProblemSet] = useState(true);
  const [existingProblem, setExistingProblem] = useState(false);
  const [problemSets, setProblemSets] = useState<ProblemSetSummary[]>([]);
  const [problemOptions, setProblemOptions] = useState<string[]>([]);
  const [selectedProblemSetId, setSelectedProblemSetId] = useState<string | null>(null);
  const [selectedProblemId, setSelectedProblemId] = useState<string | null>(null);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>('postgresql');
  const [loadedProblemSetDetail, setLoadedProblemSetDetail] = useState<ProblemSetDetailData>(EMPTY_PROBLEM_SET_DETAIL);
  const [loadedProblemDetail, setLoadedProblemDetail] = useState<ProblemDetailData | null>(null);
  const [isProblemSetLoading, setIsProblemSetLoading] = useState(false);
  const [isProblemLoading, setIsProblemLoading] = useState(false);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [problemSetErrorMessage, setProblemSetErrorMessage] = useState('');
  const [problemErrorMessage, setProblemErrorMessage] = useState('');
  const [previewErrorMessage, setPreviewErrorMessage] = useState('');
  const [previewData, setPreviewData] = useState<ProblemOutputPreviewData>(EMPTY_PREVIEW_OUTPUT);
  const [includedTableNames, setIncludedTableNames] = useState<string[]>([]);
  const [popupState, setPopupState] = useState<PopupState>({ open: false, level: 2, message: '' });
  const [collapsedSections, setCollapsedSections] = useState<Record<SectionKey, boolean>>({
    condition: false,
    output: false,
    ddl: false,
    actualData: false,
    sampleData: false,
    outputPreview: false,
    answerSql: false,
  });

  const currentDbms = useMemo<DbmsType>(() => {
    if (!existingProblemSet) {
      return selectedDbms;
    }

    return resolveScopedDbms(selectedProblemId ?? selectedProblemSetId);
  }, [existingProblemSet, selectedDbms, selectedProblemId, selectedProblemSetId]);

  const currentFullProblemSetDdl = useMemo(
    () => (currentDbms === 'mysql' ? loadedProblemSetDetail.ddlMysql : loadedProblemSetDetail.ddlPostgresql),
    [currentDbms, loadedProblemSetDetail.ddlMysql, loadedProblemSetDetail.ddlPostgresql],
  );
  const currentFullActualData = useMemo(
    () => (currentDbms === 'mysql' ? loadedProblemSetDetail.dataMysql : loadedProblemSetDetail.dataPostgresql),
    [currentDbms, loadedProblemSetDetail.dataMysql, loadedProblemSetDetail.dataPostgresql],
  );

  const availableTableNames = useMemo(() => {
    if (!existingProblemSet) {
      return [];
    }

    return getTableNamesFromDdl(currentFullProblemSetDdl);
  }, [currentFullProblemSetDdl, existingProblemSet]);

  useEffect(() => {
    if (availableTableNames.length === 0) {
      setIncludedTableNames([]);
      return;
    }

    if (existingProblemSet && existingProblem && loadedProblemDetail) {
      const problemDdl = currentDbms === 'mysql' ? loadedProblemDetail.ddlMysql : loadedProblemDetail.ddlPostgresql;
      const nextIncludedTableNames = getTableNamesFromDdl(problemDdl).filter((tableName) => availableTableNames.includes(tableName));
      const normalizedIncludedTableNames = nextIncludedTableNames.length > 0 ? nextIncludedTableNames : availableTableNames;
      setIncludedTableNames((current) => (arraysEqual(current, normalizedIncludedTableNames) ? current : normalizedIncludedTableNames));
      return;
    }

    setIncludedTableNames((current) => {
      const filtered = current.filter((tableName) => availableTableNames.includes(tableName));
      const nextIncludedTableNames = filtered.length > 0 ? filtered : availableTableNames;
      return arraysEqual(current, nextIncludedTableNames) ? current : nextIncludedTableNames;
    });
  }, [availableTableNames, currentDbms, loadedProblemDetail, existingProblem, existingProblemSet]);

  const scopedProblemSetDdl = useMemo(() => {
    if (!existingProblemSet) {
      return currentDbms === 'mysql' ? ddlState.appliedValue.mysql : ddlState.appliedValue.postgresql;
    }

    return filterDdlByTableNames(currentFullProblemSetDdl, includedTableNames);
  }, [currentDbms, currentFullProblemSetDdl, ddlState.appliedValue.mysql, ddlState.appliedValue.postgresql, includedTableNames, existingProblemSet]);

  const currentActualData = useMemo(() => {
    if (!existingProblemSet) {
      return currentDbms === 'mysql' ? actualDataState.appliedValue.mysql : actualDataState.appliedValue.postgresql;
    }

    return currentFullActualData;
  }, [actualDataState.appliedValue.mysql, actualDataState.appliedValue.postgresql, currentDbms, currentFullActualData, existingProblemSet]);

  useEffect(() => {
    let cancelled = false;

    async function loadProblemSets() {
      try {
        const nextProblemSets = await fetchProblemSets();
        if (cancelled) return;

        setProblemSets(nextProblemSets);
        if (nextProblemSets.length === 0) {
          setExistingProblemSet(false);
          setSelectedProblemSetId(null);
          return;
        }

        setSelectedProblemSetId((current) => current ?? nextProblemSets[0].problemSetId);
      } catch (error) {
        if (!cancelled) {
          setExistingProblemSet(false);
          setSelectedProblemSetId(null);
          setProblemSetErrorMessage(error instanceof Error ? error.message : text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
        }
      }
    }

    void loadProblemSets();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!existingProblemSet || !selectedProblemSetId) {
      setProblemOptions([]);
      setSelectedProblemId(null);
      setLoadedProblemSetDetail(EMPTY_PROBLEM_SET_DETAIL);
      setLoadedProblemDetail(null);
      setExistingProblem(false);
      setProblemSetErrorMessage('');
      setProblemErrorMessage('');
      return;
    }

    const targetProblemSetId = selectedProblemSetId;
    const targetProblemSetDbms = resolveScopedDbms(targetProblemSetId);
    let cancelled = false;

    async function loadProblemManagementTargets() {
      setIsProblemSetLoading(true);
      setProblemSetErrorMessage('');

      try {
        const [nextDetail, nextProblemOptions] = await Promise.all([
          fetchProblemSetDetail(targetProblemSetId),
          fetchAdminProblemOptions(targetProblemSetId),
        ]);

        if (cancelled) return;

        setLoadedProblemSetDetail(nextDetail);
        setProblemOptions(nextProblemOptions);

        if (nextProblemOptions.length === 0) {
          setExistingProblem(false);
          setSelectedProblemId(null);
          resetProblemDrafts(targetProblemSetDbms === 'mysql' ? nextDetail.dataMysql : nextDetail.dataPostgresql);
          return;
        }

        setExistingProblem(true);
        setSelectedProblemId((current) => (current != null && nextProblemOptions.includes(current) ? current : nextProblemOptions[0]));
      } catch (error) {
        if (!cancelled) {
          setLoadedProblemSetDetail(EMPTY_PROBLEM_SET_DETAIL);
          setProblemOptions([]);
          setExistingProblem(false);
          setSelectedProblemId(null);
          setProblemSetErrorMessage(error instanceof Error ? error.message : text('PROBLEM_CREATE_SET_DETAIL_FAIL_MESSAGE', '테이블셋 정보를 불러오지 못했습니다.'));
        }
      } finally {
        if (!cancelled) {
          setIsProblemSetLoading(false);
        }
      }
    }

    void loadProblemManagementTargets();
    return () => {
      cancelled = true;
    };
  }, [existingProblemSet, selectedProblemSetId]);

  useEffect(() => {
    if (!existingProblemSet || !existingProblem || !selectedProblemId) {
      setLoadedProblemDetail(null);
      setProblemErrorMessage('');
      return;
    }

    const targetProblemId = selectedProblemId;
    let cancelled = false;

    async function loadProblemDetailData() {
      setIsProblemLoading(true);
      setProblemErrorMessage('');

      try {
        const nextProblemDetail = await fetchProblemDetail(targetProblemId);
        if (cancelled) return;

        setLoadedProblemDetail(nextProblemDetail);
        heroState.replaceValue({ title: nextProblemDetail.title, description: nextProblemDetail.description });
        conditionState.replaceValue(nextProblemDetail.condition);
        outputState.replaceValue(nextProblemDetail.output);
        sampleDataState.replaceValue(nextProblemDetail.sampleDataSql);
        answerSqlState.replaceValue(nextProblemDetail.answerSql);
        setPreviewData(parseProblemOutputSample(nextProblemDetail.outputSample));
        setPreviewErrorMessage('');
      } catch (error) {
        if (!cancelled) {
          setLoadedProblemDetail(null);
          setProblemErrorMessage(error instanceof Error ? error.message : text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
        }
      } finally {
        if (!cancelled) {
          setIsProblemLoading(false);
        }
      }
    }

    void loadProblemDetailData();
    return () => {
      cancelled = true;
    };
  }, [existingProblem, existingProblemSet, selectedProblemId]);

  useEffect(() => {
    if (existingProblemSet) {
      return;
    }

    resetProblemDrafts('');
  }, [existingProblemSet]);

  useEffect(() => {
    if (!existingProblemSet || existingProblem) {
      return;
    }

    if (sampleDataState.appliedValue.trim() !== '') {
      return;
    }

    const nextSampleData = filterDataSqlByTableNames(currentFullActualData, includedTableNames);
    if (nextSampleData.trim() !== '') {
      sampleDataState.replaceValue(nextSampleData);
    }
  }, [currentFullActualData, includedTableNames, existingProblem, existingProblemSet, sampleDataState.appliedValue]);

  const selectedProblemSetValue = existingProblemSet && selectedProblemSetId != null ? selectedProblemSetId : NEW_PROBLEM_SET_OPTION_VALUE;
  const selectedProblemValue = existingProblem && selectedProblemId != null ? selectedProblemId : NEW_PROBLEM_OPTION_VALUE;
  const immutableProblemSql = existingProblem;

  const missingFields = buildMissingFields({
    title: heroState.appliedValue.title,
    description: heroState.appliedValue.description,
    condition: conditionState.appliedValue,
    output: outputState.appliedValue,
    ddl: scopedProblemSetDdl,
    actualData: currentActualData,
    sampleData: sampleDataState.appliedValue,
    answerSql: answerSqlState.appliedValue,
  });

  const missingFieldLabels = missingFields.map((field) => field.label);
  const completedFieldCount = 8 - missingFields.length;

  function resetProblemDrafts(nextSampleData: string) {
    heroState.replaceValue({ title: '', description: '' });
    conditionState.replaceValue('');
    outputState.replaceValue('');
    sampleDataState.replaceValue(nextSampleData);
    answerSqlState.replaceValue('');
    setPreviewData(EMPTY_PREVIEW_OUTPUT);
    setPreviewErrorMessage('');
  }

  function toggleSection(sectionKey: SectionKey) {
    setCollapsedSections((current) => ({
      ...current,
      [sectionKey]: !current[sectionKey],
    }));
  }

  function openSection(sectionKey: SectionKey) {
    setCollapsedSections((current) => ({
      ...current,
      [sectionKey]: false,
    }));
  }

  function scrollToElement(element: HTMLElement | null) {
    if (!element) {
      return;
    }

    window.requestAnimationFrame(() => {
      element.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    });
  }

  function scrollToMissingField(fieldKey: MissingFieldKey) {
    if (fieldKey === 'title' || fieldKey === 'description') {
      if (!heroState.isEditing) {
        heroState.startEditing();
      }
      scrollToElement(heroSectionRef.current);
      return;
    }

    const sectionKey = fieldKey === 'ddl' ? 'ddl'
      : fieldKey === 'actualData' ? 'actualData'
      : fieldKey === 'sampleData' ? 'sampleData'
      : fieldKey === 'answerSql' ? 'answerSql'
      : fieldKey;

    openSection(sectionKey);
    scrollToElement(sectionRefs.current[sectionKey]);
  }

  function handleProblemSetSelectChange(nextValue: string) {
    if (nextValue === NEW_PROBLEM_SET_OPTION_VALUE) {
      setExistingProblemSet(false);
      setExistingProblem(false);
      setSelectedProblemSetId(null);
      setSelectedProblemId(null);
      return;
    }

    setExistingProblemSet(true);
    setSelectedProblemSetId(nextValue);
  }

  function handleProblemSelectChange(nextValue: string) {
    if (nextValue === NEW_PROBLEM_OPTION_VALUE) {
      setExistingProblem(false);
      setSelectedProblemId(null);
      resetProblemDrafts(filterDataSqlByTableNames(currentFullActualData, includedTableNames));
      return;
    }

    setExistingProblem(true);
    setSelectedProblemId(nextValue);
  }

  async function handlePreviewOutput() {
    const previewMissingField = buildMissingFields({
      title: heroState.appliedValue.title || 'filled',
      description: heroState.appliedValue.description || 'filled',
      condition: conditionState.appliedValue || 'filled',
      output: outputState.appliedValue || 'filled',
      ddl: scopedProblemSetDdl,
      actualData: currentActualData || 'filled',
      sampleData: sampleDataState.appliedValue,
      answerSql: answerSqlState.appliedValue,
    }).find((field) => field.key === 'ddl' || field.key === 'sampleData' || field.key === 'answerSql');

    if (previewMissingField) {
      scrollToMissingField(previewMissingField.key);
      return;
    }

    setIsPreviewLoading(true);
    setPreviewErrorMessage('');

    try {
      const nextPreview = await previewProblemOutput({
        dbms: currentDbms,
        ddl: scopedProblemSetDdl,
        sampleDataSql: sampleDataState.appliedValue.trim(),
        answerSql: answerSqlState.appliedValue.trim(),
      });
      setPreviewData(nextPreview);
    } catch (error) {
      setPreviewErrorMessage(error instanceof Error ? error.message : text('PROBLEM_CREATE_PREVIEW_FAIL_MESSAGE', '출력 예시를 생성하지 못했습니다.'));
    } finally {
      setIsPreviewLoading(false);
    }
  }

  async function handleCreateProblem() {
    if (missingFields.length > 0) {
      scrollToMissingField(missingFields[0].key);
      return;
    }

    setIsSaving(true);

    try {
      const createdProblemId = await createProblem({
        title: heroState.appliedValue.title.trim(),
        description: heroState.appliedValue.description.trim(),
        condition: conditionState.appliedValue.trim(),
        output: outputState.appliedValue.trim(),
        ddl: scopedProblemSetDdl,
        actualDataSql: currentActualData.trim(),
        sampleDataSql: sampleDataState.appliedValue.trim(),
        answerSql: answerSqlState.appliedValue.trim(),
        problemSetId: existingProblemSet ? selectedProblemSetId ?? undefined : undefined,
        problemId: existingProblem ? selectedProblemId ?? undefined : undefined,
        dbms: currentDbms,
      });

      navigate(`/problems/${encodeURIComponent(createdProblemId)}`);
    } catch (error) {
      setPopupState({
        open: true,
        level: 2,
        message: error instanceof Error ? error.message : text('PROBLEM_CREATE_SAVE_FAIL_MESSAGE', '문제를 저장하지 못했습니다.'),
      });
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <>
      <div ref={pageRef} className="page-stack problem-create-page">
        <section ref={heroSectionRef} className="solve-page-hero solve-surface-section problem-create-hero">
          <div className="solve-page-hero-copy solve-page-hero-copy-wide">
            <div className="problem-create-number-row">
              <span className="solve-problem-number">{getProblemNumberLabel(existingProblemSet, existingProblem, selectedProblemSetId, selectedProblemId)}</span>
              <ProblemCreateSelectMenu
                value={selectedProblemSetValue}
                className="problem-create-select"
                options={[
                  { value: NEW_PROBLEM_SET_OPTION_VALUE, label: text('PROBLEM_CREATE_NEW_SET_LABEL', '신규 테이블셋') },
                  ...problemSets.map((problemSet) => ({
                    value: problemSet.problemSetId,
                    label: getProblemSetLabel(problemSet.problemSetId),
                  })),
                ]}
                onChange={handleProblemSetSelectChange}
              />
              {!existingProblemSet ? (
                <ProblemCreateSelectMenu
                  value={selectedDbms}
                  className="problem-create-select problem-create-problem-select"
                  options={[
                    { value: 'postgresql', label: text('COMMON_POSTGRESQL_LABEL', 'PostgreSQL') },
                    { value: 'mysql', label: text('COMMON_MYSQL_LABEL', 'MySQL') },
                  ]}
                  onChange={(nextValue) => setSelectedDbms(nextValue as DbmsType)}
                />
              ) : (
                <ProblemCreateSelectMenu
                  value={selectedProblemValue}
                  className="problem-create-select problem-create-problem-select"
                  options={[
                    { value: NEW_PROBLEM_OPTION_VALUE, label: text('PROBLEM_CREATE_NEW_PROBLEM_LABEL', '신규 문제') },
                    ...problemOptions.map((problemId) => ({
                      value: problemId,
                      label: getProblemLabel(problemId),
                    })),
                  ]}
                  onChange={handleProblemSelectChange}
                  disabled={problemOptions.length === 0}
                />
              )}
            </div>

            <div className="problem-create-title-edit-row">
              {heroState.isEditing ? (
                <input
                  className="problem-create-title-input"
                  value={heroState.draftValue.title}
                  onChange={(event) => heroState.setDraftValue((current) => ({ ...current, title: event.target.value }))}
                  placeholder={text('PROBLEM_CREATE_TITLE_PLACEHOLDER', '문제 제목')}
                />
              ) : (
                <h1 className="solve-problem-title">
                  {heroState.appliedValue.title.trim() !== '' ? heroState.appliedValue.title : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_TITLE_LABEL', '문제 제목')}</span>}
                </h1>
              )}
              <InlineEditActions isEditing={heroState.isEditing} onEdit={heroState.startEditing} onCancel={heroState.cancelEditing} onConfirm={heroState.confirmEditing} />
            </div>

            {heroState.isEditing ? (
              <textarea
                className="text-field problem-create-inline-textarea problem-create-description-editor"
                value={heroState.draftValue.description}
                onChange={(event) => heroState.setDraftValue((current) => ({ ...current, description: event.target.value }))}
                placeholder={text('PROBLEM_CREATE_DESCRIPTION_PLACEHOLDER', '설명')}
              />
            ) : (
              <p className="solve-problem-description">
                {heroState.appliedValue.description.trim() !== '' ? heroState.appliedValue.description : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_DESCRIPTION_LABEL', '설명')}</span>}
              </p>
            )}
          </div>
        </section>

        {existingProblemSet && isProblemSetLoading ? <p className="problem-create-info">{text('PROBLEM_CREATE_SET_LOADING_LABEL', '테이블셋 정보를 불러오는 중입니다.')}</p> : null}
        {existingProblemSet && problemSetErrorMessage ? <p className="problem-create-error">{problemSetErrorMessage}</p> : null}
        {existingProblem && isProblemLoading ? <p className="problem-create-info">{text('PROBLEM_CREATE_PROBLEM_LOADING_LABEL', '문제 정보를 불러오는 중입니다.')}</p> : null}
        {existingProblem && problemErrorMessage ? <p className="problem-create-error">{problemErrorMessage}</p> : null}

        {existingProblemSet && availableTableNames.length > 0 ? (
          <section className="problem-create-table-selector-card">
            <div className="problem-create-table-selector-header">
              <h2>{text('PROBLEM_CREATE_SET_SCOPE_TITLE', '테이블 범위')}</h2>
              <p>{text('PROBLEM_CREATE_SET_SCOPE_DESC', '기존 테이블셋에서 문제에 보여 줄 테이블 범위를 선택합니다.')}</p>
            </div>
            <div className="problem-create-table-chip-row">
              {availableTableNames.map((tableName) => (
                <label key={tableName} className="problem-create-table-chip">
                  <input
                    type="checkbox"
                    checked={includedTableNames.includes(tableName)}
                    disabled={immutableProblemSql}
                    onChange={() =>
                      setIncludedTableNames((current) =>
                        current.includes(tableName) ? current.filter((item) => item !== tableName) : [...current, tableName],
                      )
                    }
                  />
                  <span>{tableName}</span>
                </label>
              ))}
            </div>
          </section>
        ) : null}

        <ProblemCreateSection
          sectionKey="condition"
          title={text('PROBLEM_CREATE_CONDITION_LABEL', '조건')}
          collapsed={collapsedSections.condition}
          onToggle={toggleSection}
          isMissing={missingFields.some((field) => field.key === 'condition')}
          actions={<InlineEditActions isEditing={conditionState.isEditing} onEdit={conditionState.startEditing} onCancel={conditionState.cancelEditing} onConfirm={conditionState.confirmEditing} />}
          sectionRef={(node) => {
            sectionRefs.current.condition = node;
          }}
        >
          {conditionState.isEditing ? (
            <textarea className="text-field problem-create-inline-textarea" value={conditionState.draftValue} onChange={(event) => conditionState.setDraftValue(event.target.value)} placeholder={text('PROBLEM_CREATE_CONDITION_LABEL', '조건')} />
          ) : (
            <pre className="problem-create-code-preview">{conditionState.appliedValue.trim() !== '' ? conditionState.appliedValue : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_CONDITION_LABEL', '조건')}</span>}</pre>
          )}
        </ProblemCreateSection>

        <ProblemCreateSection
          sectionKey="output"
          title={text('PROBLEM_CREATE_OUTPUT_LABEL', '출력 설명')}
          collapsed={collapsedSections.output}
          onToggle={toggleSection}
          isMissing={missingFields.some((field) => field.key === 'output')}
          actions={<InlineEditActions isEditing={outputState.isEditing} onEdit={outputState.startEditing} onCancel={outputState.cancelEditing} onConfirm={outputState.confirmEditing} />}
          sectionRef={(node) => {
            sectionRefs.current.output = node;
          }}
        >
          {outputState.isEditing ? (
            <textarea className="text-field problem-create-inline-textarea" value={outputState.draftValue} onChange={(event) => outputState.setDraftValue(event.target.value)} placeholder={text('PROBLEM_CREATE_OUTPUT_LABEL', '출력 설명')} />
          ) : (
            <pre className="problem-create-code-preview">{outputState.appliedValue.trim() !== '' ? outputState.appliedValue : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_OUTPUT_LABEL', '출력 설명')}</span>}</pre>
          )}
        </ProblemCreateSection>

        <ProblemCreateSection
          sectionKey="ddl"
          title={text('PROBLEM_CREATE_DDL_LABEL', '테이블 정보 DDL')}
          collapsed={collapsedSections.ddl}
          onToggle={toggleSection}
          isMissing={missingFields.some((field) => field.key === 'ddl')}
          actions={
            !existingProblemSet ? (
              <InlineEditActions isEditing={ddlState.isEditing} onEdit={ddlState.startEditing} onCancel={ddlState.cancelEditing} onConfirm={ddlState.confirmEditing} />
            ) : null
          }
          sectionRef={(node) => {
            sectionRefs.current.ddl = node;
          }}
        >
          {!existingProblemSet && ddlState.isEditing ? (
            <textarea
              className="text-field problem-create-code-textarea"
              value={currentDbms === 'mysql' ? ddlState.draftValue.mysql : ddlState.draftValue.postgresql}
              onChange={(event) =>
                ddlState.setDraftValue((current) =>
                  currentDbms === 'mysql' ? { ...current, mysql: event.target.value } : { ...current, postgresql: event.target.value },
                )
              }
              placeholder={currentDbms === 'mysql' ? 'MySQL DDL' : 'PostgreSQL DDL'}
            />
          ) : (
            <pre className="problem-create-code-preview">{scopedProblemSetDdl.trim() !== '' ? scopedProblemSetDdl : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_DDL_LABEL', '테이블 정보 DDL')}</span>}</pre>
          )}
        </ProblemCreateSection>

        <ProblemCreateSection
          sectionKey="actualData"
          title={text('PROBLEM_CREATE_ACTUAL_DATA_TITLE', '실제 채점 데이터 INSERT')}
          collapsed={collapsedSections.actualData}
          onToggle={toggleSection}
          isMissing={missingFields.some((field) => field.key === 'actualData')}
          actions={
            !existingProblemSet ? (
              <InlineEditActions isEditing={actualDataState.isEditing} onEdit={actualDataState.startEditing} onCancel={actualDataState.cancelEditing} onConfirm={actualDataState.confirmEditing} />
            ) : null
          }
          sectionRef={(node) => {
            sectionRefs.current.actualData = node;
          }}
        >
          {!existingProblemSet && actualDataState.isEditing ? (
            <textarea
              className="text-field problem-create-code-textarea"
              value={currentDbms === 'mysql' ? actualDataState.draftValue.mysql : actualDataState.draftValue.postgresql}
              onChange={(event) =>
                actualDataState.setDraftValue((current) =>
                  currentDbms === 'mysql' ? { ...current, mysql: event.target.value } : { ...current, postgresql: event.target.value },
                )
              }
              placeholder={currentDbms === 'mysql' ? text('PROBLEM_CREATE_ACTUAL_DATA_MYSQL_PLACEHOLDER', 'MySQL 실제 채점 데이터 INSERT') : text('PROBLEM_CREATE_ACTUAL_DATA_POSTGRES_PLACEHOLDER', 'PostgreSQL 실제 채점 데이터 INSERT')}
            />
          ) : (
            <pre className="problem-create-code-preview">{currentActualData.trim() !== '' ? currentActualData : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_ACTUAL_DATA_TITLE', '실제 채점 데이터 INSERT')}</span>}</pre>
          )}
        </ProblemCreateSection>

        <ProblemCreateSection
          sectionKey="sampleData"
          title={text('PROBLEM_CREATE_SAMPLE_DATA_TITLE', '예시 데이터 INSERT')}
          collapsed={collapsedSections.sampleData}
          onToggle={toggleSection}
          isMissing={missingFields.some((field) => field.key === 'sampleData')}
          actions={
            !immutableProblemSql ? (
              <InlineEditActions isEditing={sampleDataState.isEditing} onEdit={sampleDataState.startEditing} onCancel={sampleDataState.cancelEditing} onConfirm={sampleDataState.confirmEditing} />
            ) : null
          }
          sectionRef={(node) => {
            sectionRefs.current.sampleData = node;
          }}
        >
          {!immutableProblemSql && sampleDataState.isEditing ? (
            <textarea className="text-field problem-create-code-textarea" value={sampleDataState.draftValue} onChange={(event) => sampleDataState.setDraftValue(event.target.value)} placeholder={text('PROBLEM_CREATE_SAMPLE_DATA_TITLE', '예시 데이터 INSERT')} />
          ) : (
            <pre className="problem-create-code-preview">{sampleDataState.appliedValue.trim() !== '' ? sampleDataState.appliedValue : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_SAMPLE_DATA_TITLE', '예시 데이터 INSERT')}</span>}</pre>
          )}
        </ProblemCreateSection>

        <ProblemCreateSection
          sectionKey="outputPreview"
          title={text('PROBLEM_CREATE_OUTPUT_SAMPLE_TITLE', '출력 예시')}
          collapsed={collapsedSections.outputPreview}
          onToggle={toggleSection}
        >
          {previewErrorMessage ? <p className="problem-create-error">{previewErrorMessage}</p> : null}
          <ProblemCreatePreviewGrid previewData={previewData} />
        </ProblemCreateSection>

        <ProblemCreateSection
          sectionKey="answerSql"
          title={text('PROBLEM_CREATE_ANSWER_SQL_TITLE', '정답 SQL')}
          collapsed={collapsedSections.answerSql}
          onToggle={toggleSection}
          isMissing={missingFields.some((field) => field.key === 'answerSql')}
          actions={
            !immutableProblemSql ? (
              <InlineEditActions isEditing={answerSqlState.isEditing} onEdit={answerSqlState.startEditing} onCancel={answerSqlState.cancelEditing} onConfirm={answerSqlState.confirmEditing} />
            ) : null
          }
          sectionRef={(node) => {
            sectionRefs.current.answerSql = node;
          }}
        >
          {!immutableProblemSql && answerSqlState.isEditing ? (
            <textarea className="text-field problem-create-code-textarea problem-create-answer-textarea" value={answerSqlState.draftValue} onChange={(event) => answerSqlState.setDraftValue(event.target.value)} placeholder={text('PROBLEM_CREATE_ANSWER_SQL_LABEL', '정답 SQL')} />
          ) : (
            <pre className="problem-create-code-preview problem-create-answer-preview">{answerSqlState.appliedValue.trim() !== '' ? answerSqlState.appliedValue : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_ANSWER_SQL_LABEL', '정답 SQL')}</span>}</pre>
          )}
        </ProblemCreateSection>
      </div>

      <div className="problem-create-sticky-bar">
        <div className="problem-create-sticky-status">
          {missingFields.length === 0 ? (
            <p>{text('PROBLEM_CREATE_COMPLETED_FIELDS_LABEL', { count: completedFieldCount }, `필수 항목 ${completedFieldCount}/8 완료`)}</p>
          ) : (
            <p>{text('PROBLEM_CREATE_MISSING_FIELDS_LABEL', { fields: missingFieldLabels.join(', ') }, `필수 항목 누락: ${missingFieldLabels.join(', ')}`)}</p>
          )}
        </div>
        <div className="problem-create-sticky-actions">
          <button type="button" className="btn secondary" onClick={() => void handlePreviewOutput()} disabled={isPreviewLoading}>
            {isPreviewLoading ? text('PROBLEM_CREATE_OUTPUT_GENERATING_LABEL', '출력 예시 생성 중') : text('PROBLEM_CREATE_OUTPUT_GENERATE_BUTTON', '출력 예시 생성')}
          </button>
          <button type="button" className="btn primary problem-create-submit-button" onClick={() => void handleCreateProblem()} disabled={isSaving}>
            {isSaving ? text('PROBLEM_CREATE_SAVING_LABEL', '문제 저장 중') : text('PROBLEM_CREATE_CREATE_BUTTON', '문제 생성')}
          </button>
        </div>
      </div>

      <StatusPopup open={popupState.open} level={popupState.level} message={popupState.message} onConfirm={() => setPopupState((current) => ({ ...current, open: false }))} />
    </>
  );
}

export default function ProblemCreatePage() {
  return <ProblemCreateContent />;
}
