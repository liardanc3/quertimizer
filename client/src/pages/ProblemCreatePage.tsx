import { useEffect, useMemo, useRef, useState, type Dispatch, type SetStateAction } from 'react';
import StatusPopup from '../components/common/StatusPopup';
import ProblemDetailContent from '../components/problem/ProblemDetailContent';
import {
  createProblem,
  fetchAdminProblemOptions,
  fetchProblemDetail,
  fetchProblemSetDetail,
  fetchProblemSets,
  type DbmsType,
  type ProblemDetailData,
  type ProblemSetDetailData,
  type ProblemSetSummary,
} from '../lib/problemApi';
import { navigate } from '../lib/navigation';
import './ProblemCreatePage.css';

type ProblemSetMode = 'existing' | 'new';
type ProblemMode = 'existing' | 'new';

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
  oracle: string;
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

type MissingFieldKey =
  | 'title'
  | 'description'
  | 'condition'
  | 'output'
  | 'outputSample'
  | 'answer'
  | 'tableInfo'
  | 'dataSample';

interface MissingField {
  key: MissingFieldKey;
  label: string;
}

const EMPTY_PROBLEM_SET_DETAIL: ProblemSetDetailData = {
  problemSetId: '',
  ddlPostgresql: '',
  ddlOracle: '',
  dataPostgresql: '',
  dataOracle: '',
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
  return `테이블셋 ${problemSetId}`;
}

function getProblemLabel(problemId: string) {
  return `문제 ${problemId}`;
}

function getProblemNumberLabel(problemSetMode: ProblemSetMode, problemMode: ProblemMode, problemSetId: string | null, problemId: string | null) {
  if (problemSetMode === 'existing' && problemMode === 'existing' && problemId) {
    return problemId;
  }

  if (problemSetMode === 'existing' && problemSetId) {
    return `${problemSetId}-신규`;
  }

  return '신규 문제';
}

function resolveScopedDbms(value: string | null | undefined): DbmsType {
  return value?.trim().startsWith('O') ? 'oracle' : 'postgresql';
}

function createEmptyProblemDetail(dbms: DbmsType): ProblemDetailData {
  return {
    problemId: '',
    title: '',
    description: '',
    ddlPostgresql: '',
    ddlOracle: '',
    dataPostgresql: '',
    dataOracle: '',
    condition: '',
    output: '',
    outputSample: '',
    answer: '',
    answerHash: '',
    dbms,
  };
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
    return '';
  }

  const tableNameSet = new Set(includedTableNames);
  const createTablePattern = /CREATE TABLE\s+(?:[\w]+\.)?(\w+)\s*\([\s\S]*?\);/gi;
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
    return '';
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

function buildSectionClassName(isMissing: boolean) {
  return `problem-create-validatable-section${isMissing ? ' problem-create-missing-section' : ''}`;
}

function arraysEqual(left: string[], right: string[]) {
  if (left.length !== right.length) {
    return false;
  }

  return left.every((value, index) => value === right[index]);
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

function InlineEditActions({ isEditing, onEdit, onCancel, onConfirm }: { isEditing: boolean; onEdit: () => void; onCancel: () => void; onConfirm: () => void }) {
  return (
    <div className="problem-create-inline-toolbar">
      <button type="button" className={`solve-detail-section-action problem-create-edit-action ${isEditing ? 'is-active' : ''}`.trim()} aria-label={isEditing ? '수정 취소' : '수정'} onClick={isEditing ? onCancel : onEdit}>
        {isEditing ? <CloseIcon /> : <EditIcon />}
      </button>
      {isEditing ? (
        <button type="button" className="solve-detail-section-action problem-create-confirm-action" aria-label="수정 적용" onClick={onConfirm}>
          <CheckIcon />
        </button>
      ) : null}
    </div>
  );
}

function SelectChevronIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="m5.5 7.8 4.5 4.4 4.5-4.4" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
    </svg>
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

  const selectedOption = options.find((option) => option.value === value) ?? options[0];

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    function handleDocumentMouseDown(event: MouseEvent) {
      if (!(event.target instanceof Node)) {
        return;
      }

      if (!shellRef.current?.contains(event.target)) {
        setIsOpen(false);
      }
    }

    function handleEscape(event: globalThis.KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    }

    document.addEventListener('mousedown', handleDocumentMouseDown);
    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('mousedown', handleDocumentMouseDown);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen]);

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

export function ProblemCreateContent() {
  const pageRef = useRef<HTMLDivElement | null>(null);
  const heroSectionRef = useRef<HTMLElement | null>(null);
  const answerSectionRef = useRef<HTMLElement | null>(null);
  const heroState = useEditableDraft({ title: '', description: '' });
  const conditionState = useEditableDraft('');
  const outputState = useEditableDraft('');
  const outputSampleState = useEditableDraft('');
  const answerState = useEditableDraft('');
  const ddlState = useEditableDraft<SourceDraft>({ postgresql: '', oracle: '' });
  const dataState = useEditableDraft<SourceDraft>({ postgresql: '', oracle: '' });

  const [problemSetMode, setProblemSetMode] = useState<ProblemSetMode>('existing');
  const [problemMode, setProblemMode] = useState<ProblemMode>('new');
  const [problemSets, setProblemSets] = useState<ProblemSetSummary[]>([]);
  const [problemOptions, setProblemOptions] = useState<string[]>([]);
  const [selectedProblemSetId, setSelectedProblemSetId] = useState<string | null>(null);
  const [selectedProblemId, setSelectedProblemId] = useState<string | null>(null);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>('postgresql');
  const [loadedProblemSetDetail, setLoadedProblemSetDetail] = useState<ProblemSetDetailData>(EMPTY_PROBLEM_SET_DETAIL);
  const [loadedProblemDetail, setLoadedProblemDetail] = useState<ProblemDetailData | null>(null);
  const [answerHash, setAnswerHash] = useState('');
  const [isProblemSetLoading, setIsProblemSetLoading] = useState(false);
  const [isProblemLoading, setIsProblemLoading] = useState(false);
  const [problemSetErrorMessage, setProblemSetErrorMessage] = useState('');
  const [problemErrorMessage, setProblemErrorMessage] = useState('');
  const [includedTableNames, setIncludedTableNames] = useState<string[]>([]);
  const [editorSql, setEditorSql] = useState('');
  const [popupState, setPopupState] = useState<PopupState>({ open: false, level: 2, message: '' });

  const currentDbms = useMemo<DbmsType>(() => {
    if (problemSetMode === 'new') {
      return selectedDbms;
    }

    return resolveScopedDbms(selectedProblemId ?? selectedProblemSetId);
  }, [problemSetMode, selectedDbms, selectedProblemId, selectedProblemSetId]);

  function replaceProblemContent(detail: ProblemDetailData) {
    heroState.replaceValue({ title: detail.title, description: detail.description });
    conditionState.replaceValue(detail.condition);
    outputState.replaceValue(detail.output);
    outputSampleState.replaceValue(detail.outputSample);
    answerState.replaceValue(detail.answer);
    setAnswerHash(detail.answerHash);
    setEditorSql(detail.answer);
  }

  useEffect(() => {
    let cancelled = false;

    async function loadProblemSets() {
      try {
        const nextProblemSets = await fetchProblemSets();
        if (cancelled) return;

        setProblemSets(nextProblemSets);
        if (nextProblemSets.length === 0) {
          setProblemSetMode('new');
          setSelectedProblemSetId(null);
          return;
        }

        setSelectedProblemSetId((current) => current ?? nextProblemSets[0].problemSetId);
      } catch {
        if (!cancelled) {
          setProblemSetMode('new');
          setSelectedProblemSetId(null);
          setProblemSetErrorMessage('테이블셋 목록을 불러오지 못했다.');
        }
      }
    }

    loadProblemSets();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (problemSetMode !== 'existing' || !selectedProblemSetId) {
      setProblemOptions([]);
      setSelectedProblemId(null);
      setLoadedProblemSetDetail(EMPTY_PROBLEM_SET_DETAIL);
      setLoadedProblemDetail(null);
      setProblemMode('new');
      setProblemSetErrorMessage('');
      setProblemErrorMessage('');
      setAnswerHash('');
      replaceProblemContent(createEmptyProblemDetail(currentDbms));
      return;
    }

    const targetProblemSetId = selectedProblemSetId;
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
          setProblemMode('new');
          setSelectedProblemId(null);
          setAnswerHash('');
          replaceProblemContent(createEmptyProblemDetail(resolveScopedDbms(targetProblemSetId)));
          return;
        }

        setProblemMode('existing');
        setSelectedProblemId((current) => (current != null && nextProblemOptions.includes(current) ? current : nextProblemOptions[0]));
      } catch {
        if (!cancelled) {
          setLoadedProblemSetDetail(EMPTY_PROBLEM_SET_DETAIL);
          setProblemOptions([]);
          setProblemMode('new');
          setSelectedProblemId(null);
          setAnswerHash('');
          setProblemSetErrorMessage('테이블셋 정보를 불러오지 못했다.');
        }
      } finally {
        if (!cancelled) {
          setIsProblemSetLoading(false);
        }
      }
    }

    loadProblemManagementTargets();
    return () => {
      cancelled = true;
    };
  }, [problemSetMode, selectedProblemSetId]);

  useEffect(() => {
    if (problemSetMode !== 'existing' || problemMode !== 'existing' || !selectedProblemId) {
      setLoadedProblemDetail(null);
      setProblemErrorMessage('');
      if (problemSetMode === 'existing') {
        setAnswerHash('');
        replaceProblemContent(createEmptyProblemDetail(currentDbms));
      }
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
        replaceProblemContent(nextProblemDetail);
      } catch {
        if (!cancelled) {
          setLoadedProblemDetail(null);
          setAnswerHash('');
          setProblemErrorMessage('문제 정보를 불러오지 못했다.');
        }
      } finally {
        if (!cancelled) {
          setIsProblemLoading(false);
        }
      }
    }

    loadProblemDetailData();
    return () => {
      cancelled = true;
    };
  }, [problemMode, problemSetMode, selectedProblemId]);

  const currentProblemSetDetail = problemSetMode === 'existing'
    ? loadedProblemSetDetail
    : {
        problemSetId: '',
        ddlPostgresql: selectedDbms === 'postgresql' ? ddlState.appliedValue.postgresql : '',
        ddlOracle: selectedDbms === 'oracle' ? ddlState.appliedValue.oracle : '',
        dataPostgresql: selectedDbms === 'postgresql' ? dataState.appliedValue.postgresql : '',
        dataOracle: selectedDbms === 'oracle' ? dataState.appliedValue.oracle : '',
      };

  const selectedProblemSetValue = problemSetMode === 'existing' && selectedProblemSetId != null
    ? selectedProblemSetId
    : NEW_PROBLEM_SET_OPTION_VALUE;
  const selectedProblemValue = problemMode === 'existing' && selectedProblemId != null
    ? selectedProblemId
    : NEW_PROBLEM_OPTION_VALUE;

  const availableTableNames = useMemo(() => {
    const targetDdl = currentDbms === 'oracle'
      ? currentProblemSetDetail.ddlOracle
      : currentProblemSetDetail.ddlPostgresql;

    return getTableNamesFromDdl(targetDdl);
  }, [currentDbms, currentProblemSetDetail.ddlOracle, currentProblemSetDetail.ddlPostgresql]);

  useEffect(() => {
    if (availableTableNames.length === 0) {
      setIncludedTableNames([]);
      return;
    }

    if (problemSetMode === 'existing' && problemMode === 'existing' && loadedProblemDetail) {
      const problemDdl = currentDbms === 'oracle'
        ? loadedProblemDetail.ddlOracle
        : loadedProblemDetail.ddlPostgresql;
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
  }, [availableTableNames, currentDbms, loadedProblemDetail, problemMode, problemSetMode]);

  const filteredDdlPostgresql = useMemo(() => filterDdlByTableNames(currentProblemSetDetail.ddlPostgresql, includedTableNames), [currentProblemSetDetail.ddlPostgresql, includedTableNames]);
  const filteredDdlOracle = useMemo(() => filterDdlByTableNames(currentProblemSetDetail.ddlOracle, includedTableNames), [currentProblemSetDetail.ddlOracle, includedTableNames]);
  const filteredDataPostgresql = useMemo(() => filterDataSqlByTableNames(currentProblemSetDetail.dataPostgresql, includedTableNames), [currentProblemSetDetail.dataPostgresql, includedTableNames]);
  const filteredDataOracle = useMemo(() => filterDataSqlByTableNames(currentProblemSetDetail.dataOracle, includedTableNames), [currentProblemSetDetail.dataOracle, includedTableNames]);

  const previewProblemSetId = problemSetMode === 'existing' && selectedProblemSetId != null
    ? selectedProblemSetId
    : selectedDbms === 'oracle'
      ? 'O신규'
      : 'P신규';

  const previewDetail = useMemo<ProblemDetailData>(
    () => ({
      problemId: selectedProblemId ?? `${previewProblemSetId}-신규`,
      title: heroState.appliedValue.title,
      description: heroState.appliedValue.description,
      ddlPostgresql: filteredDdlPostgresql,
      ddlOracle: filteredDdlOracle,
      dataPostgresql: filteredDataPostgresql,
      dataOracle: filteredDataOracle,
      condition: conditionState.appliedValue,
      output: outputState.appliedValue,
      outputSample: outputSampleState.appliedValue,
      answer: answerState.appliedValue,
      answerHash,
      dbms: currentDbms,
    }),
    [
      conditionState.appliedValue,
      currentDbms,
      filteredDataOracle,
      filteredDataPostgresql,
      filteredDdlOracle,
      filteredDdlPostgresql,
      heroState.appliedValue.description,
      heroState.appliedValue.title,
      outputSampleState.appliedValue,
      outputState.appliedValue,
      answerState.appliedValue,
      answerHash,
      previewProblemSetId,
      selectedProblemId,
    ],
  );

  const missingFields = collectMissingFields({
    title: heroState.appliedValue.title,
    description: heroState.appliedValue.description,
    condition: conditionState.appliedValue,
    output: outputState.appliedValue,
    outputSample: outputSampleState.appliedValue,
    answer: answerState.appliedValue,
    ddlPostgresql: filteredDdlPostgresql,
    ddlOracle: filteredDdlOracle,
    dataPostgresql: filteredDataPostgresql,
    dataOracle: filteredDataOracle,
  });

  const isTableInfoMissing = missingFields.some((field) => field.key === 'tableInfo');
  const isDataSampleMissing = missingFields.some((field) => field.key === 'dataSample');
  const isConditionMissing = missingFields.some((field) => field.key === 'condition');
  const isOutputMissing = missingFields.some((field) => field.key === 'output');
  const isOutputSampleMissing = missingFields.some((field) => field.key === 'outputSample');
  const isAnswerMissing = missingFields.some((field) => field.key === 'answer');

  function handleProblemSetSelectChange(nextValue: string) {

    if (nextValue === NEW_PROBLEM_SET_OPTION_VALUE) {
      setProblemSetMode('new');
      setProblemMode('new');
      setSelectedProblemSetId(null);
      setSelectedProblemId(null);
      return;
    }

    setProblemSetMode('existing');
    setSelectedProblemSetId(nextValue);
  }

  function handleProblemSelectChange(nextValue: string) {

    if (nextValue === NEW_PROBLEM_OPTION_VALUE) {
      setProblemMode('new');
      setSelectedProblemId(null);
      return;
    }

    setProblemMode('existing');
    setSelectedProblemId(nextValue);
  }

  function scrollToMissingField(fieldKey: MissingFieldKey) {
    const queryTarget = (selector: string) => pageRef.current?.querySelector<HTMLElement>(selector) ?? null;

    const scrollToTarget = (selector: string, fallback: HTMLElement | null) => {
      window.requestAnimationFrame(() => {
        window.requestAnimationFrame(() => {
          const targetElement = queryTarget(selector) ?? fallback;
          if (!targetElement) {
            return;
          }

          targetElement.scrollIntoView({
            behavior: 'smooth',
            block: 'center',
          });

          if (targetElement instanceof HTMLInputElement || targetElement instanceof HTMLTextAreaElement) {
            targetElement.focus({ preventScroll: true });
          }
        });
      });
    };

    if (fieldKey === 'title') {
      if (!heroState.isEditing) {
        heroState.startEditing();
      }
      scrollToTarget('.problem-create-title-input', heroSectionRef.current);
      return;
    }

    if (fieldKey === 'description') {
      if (!heroState.isEditing) {
        heroState.startEditing();
      }
      scrollToTarget('.problem-create-description-editor', heroSectionRef.current);
      return;
    }

    if (fieldKey === 'tableInfo') {
      if (problemSetMode === 'new' && !ddlState.isEditing) {
        ddlState.startEditing();
      }

      scrollToTarget('.solve-detail-section-table .problem-create-source-textarea', queryTarget('.solve-detail-section-table'));
      return;
    }

    if (fieldKey === 'dataSample') {
      if (problemSetMode === 'new' && !dataState.isEditing) {
        dataState.startEditing();
      }

      scrollToTarget('.solve-detail-section-data-sample .problem-create-source-textarea', queryTarget('.solve-detail-section-data-sample'));
      return;
    }

    if (fieldKey === 'condition') {
      if (!conditionState.isEditing) {
        conditionState.startEditing();
      }
      scrollToTarget('.solve-detail-section-condition .problem-create-inline-textarea', pageRef.current);
      return;
    }

    if (fieldKey === 'output') {
      if (!outputState.isEditing) {
        outputState.startEditing();
      }
      scrollToTarget('.solve-detail-section-output .problem-create-inline-textarea', pageRef.current);
      return;
    }

    if (fieldKey === 'outputSample') {
      if (!outputSampleState.isEditing) {
        outputSampleState.startEditing();
      }
      scrollToTarget('.solve-detail-section-output-sample .problem-create-inline-textarea', pageRef.current);
      return;
    }

    if (fieldKey === 'answer') {
      if (!answerState.isEditing) {
        answerState.startEditing();
      }
      scrollToTarget('.problem-create-answer-textarea', answerSectionRef.current);
    }
  }

  async function handleCreateProblem() {
    if (missingFields.length > 0) {
      scrollToMissingField(missingFields[0].key);
      return;
    }

    try {
      const normalizedAnswerSql = answerState.appliedValue.trim();
      const shouldPreserveAnswerHash =
        problemMode === 'existing' &&
        loadedProblemDetail != null &&
        normalizedAnswerSql === loadedProblemDetail.answer.trim();
      const normalizedAnswerHash = shouldPreserveAnswerHash
        ? answerHash.trim()
        : normalizedAnswerSql;

      const createdProblemId = await createProblem({
        title: heroState.appliedValue.title.trim(),
        description: heroState.appliedValue.description.trim(),
        ddlPostgresql: filteredDdlPostgresql,
        ddlOracle: filteredDdlOracle,
        condition: conditionState.appliedValue.trim(),
        output: outputState.appliedValue.trim(),
        outputSample: outputSampleState.appliedValue.trim(),
        answer: normalizedAnswerHash,
        answerSql: normalizedAnswerSql,
        problemSetMode,
        problemMode,
        problemSetId: problemSetMode === 'existing' ? selectedProblemSetId ?? undefined : undefined,
        problemId: problemMode === 'existing' ? selectedProblemId ?? undefined : undefined,
        dbms: problemSetMode === 'new' ? selectedDbms : undefined,
        dataPostgresql: problemSetMode === 'new' ? filteredDataPostgresql : undefined,
        dataOracle: problemSetMode === 'new' ? filteredDataOracle : undefined,
      });

      navigate(`/problems/${encodeURIComponent(createdProblemId)}`);
    } catch {
      setPopupState({
        open: true,
        level: 2,
        message: '문제 저장에 실패했다.',
      });
    }
  }

  return (
    <>
      <div ref={pageRef} className="page-stack problem-create-page">
        <section ref={heroSectionRef} className="solve-page-hero solve-surface-section problem-create-hero">
          <div className="solve-page-hero-copy solve-page-hero-copy-wide">
            <div className="problem-create-number-row">
              <span className="solve-problem-number">{getProblemNumberLabel(problemSetMode, problemMode, selectedProblemSetId, selectedProblemId)}</span>
              <ProblemCreateSelectMenu
                value={selectedProblemSetValue}
                className="problem-create-select"
                options={[
                  { value: NEW_PROBLEM_SET_OPTION_VALUE, label: '신규 테이블셋' },
                  ...problemSets.map((problemSet) => ({
                    value: problemSet.problemSetId,
                    label: getProblemSetLabel(problemSet.problemSetId),
                  })),
                ]}
                onChange={handleProblemSetSelectChange}
              />
              {problemSetMode === 'new' ? (
                <ProblemCreateSelectMenu
                  value={selectedDbms}
                  className="problem-create-select problem-create-problem-select problem-create-dbms-select"
                  options={[
                    { value: 'postgresql', label: 'PostgreSQL' },
                    { value: 'oracle', label: 'Oracle' },
                  ]}
                  onChange={(nextValue) => setSelectedDbms(nextValue as DbmsType)}
                />
              ) : (
                <ProblemCreateSelectMenu
                  value={selectedProblemValue}
                  className="problem-create-select problem-create-problem-select"
                  options={[
                    { value: NEW_PROBLEM_OPTION_VALUE, label: '신규 문제' },
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
                <input className="problem-create-title-input" value={heroState.draftValue.title} onChange={(event) => heroState.setDraftValue((current) => ({ ...current, title: event.target.value }))} placeholder="문제 제목" />
              ) : (
                <h1 className="solve-problem-title">
                  {heroState.appliedValue.title.trim() !== '' ? heroState.appliedValue.title : <span className="problem-create-placeholder-text">문제 제목</span>}
                </h1>
              )}
              <InlineEditActions isEditing={heroState.isEditing} onEdit={heroState.startEditing} onCancel={heroState.cancelEditing} onConfirm={heroState.confirmEditing} />
            </div>

            {heroState.isEditing ? (
              <textarea className="text-field problem-create-inline-textarea problem-create-description-editor" value={heroState.draftValue.description} onChange={(event) => heroState.setDraftValue((current) => ({ ...current, description: event.target.value }))} placeholder="설명" />
            ) : (
              <p className="solve-problem-description">
                {heroState.appliedValue.description.trim() !== '' ? heroState.appliedValue.description : <span className="problem-create-placeholder-text">설명</span>}
              </p>
            )}
          </div>
        </section>

        <ProblemDetailContent
          detail={previewDetail}
          selectedDbms={currentDbms}
          sectionClassNames={{
            table: buildSectionClassName(isTableInfoMissing),
            dataSample: buildSectionClassName(isDataSampleMissing),
            condition: buildSectionClassName(isConditionMissing),
            output: buildSectionClassName(isOutputMissing),
            outputSample: buildSectionClassName(isOutputSampleMissing),
          }}
          sectionTitleActions={{
            table: problemSetMode === 'new' ? <InlineEditActions isEditing={ddlState.isEditing} onEdit={ddlState.startEditing} onCancel={ddlState.cancelEditing} onConfirm={ddlState.confirmEditing} /> : undefined,
            dataSample: problemSetMode === 'new' ? <InlineEditActions isEditing={dataState.isEditing} onEdit={dataState.startEditing} onCancel={dataState.cancelEditing} onConfirm={dataState.confirmEditing} /> : undefined,
            condition: <InlineEditActions isEditing={conditionState.isEditing} onEdit={conditionState.startEditing} onCancel={conditionState.cancelEditing} onConfirm={conditionState.confirmEditing} />,
            output: <InlineEditActions isEditing={outputState.isEditing} onEdit={outputState.startEditing} onCancel={outputState.cancelEditing} onConfirm={outputState.confirmEditing} />,
            outputSample: <InlineEditActions isEditing={outputSampleState.isEditing} onEdit={outputSampleState.startEditing} onCancel={outputSampleState.cancelEditing} onConfirm={outputSampleState.confirmEditing} />,
          }}
          tableBeforeContent={
            <>
              {problemSetMode === 'new' && ddlState.isEditing ? (
                <div className="problem-create-inline-source">
                  <div className="problem-create-source-grid problem-create-source-grid-single">
                    <textarea
                      className="text-field problem-create-source-textarea"
                      value={selectedDbms === 'postgresql' ? ddlState.draftValue.postgresql : ddlState.draftValue.oracle}
                      onChange={(event) => ddlState.setDraftValue((current) => (
                        selectedDbms === 'postgresql'
                          ? { ...current, postgresql: event.target.value }
                          : { ...current, oracle: event.target.value }
                      ))}
                      placeholder={selectedDbms === 'postgresql' ? 'PostgreSQL DDL' : 'Oracle DDL'}
                    />
                  </div>
                </div>
              ) : null}

              {problemSetMode === 'existing' && isProblemSetLoading ? (
                <p className="content-text" aria-hidden="true">
                  <span className="wave-loading-placeholder is-medium" />
                </p>
              ) : null}
              {problemSetMode === 'existing' && problemSetErrorMessage ? <p className="problem-create-error">{problemSetErrorMessage}</p> : null}
              {problemSetMode === 'existing' && isProblemLoading ? (
                <p className="content-text" aria-hidden="true">
                  <span className="wave-loading-placeholder is-medium" />
                </p>
              ) : null}
              {problemSetMode === 'existing' && problemErrorMessage ? <p className="problem-create-error">{problemErrorMessage}</p> : null}

              {availableTableNames.length > 0 ? (
                <div className="problem-create-table-selector">
                  <div className="problem-create-table-chip-row">
                    {availableTableNames.map((tableName) => (
                      <label key={tableName} className="problem-create-table-chip">
                        <input
                          type="checkbox"
                          checked={includedTableNames.includes(tableName)}
                          onChange={() => setIncludedTableNames((current) => (
                            current.includes(tableName)
                              ? current.filter((item) => item !== tableName)
                              : [...current, tableName]
                          ))}
                        />
                        <span>{tableName}</span>
                      </label>
                    ))}
                  </div>
                </div>
              ) : null}
            </>
          }
          dataSampleBeforeContent={
            problemSetMode === 'new' && dataState.isEditing ? (
              <div className="problem-create-inline-source">
                <div className="problem-create-source-grid problem-create-source-grid-single">
                  <textarea
                    className="text-field problem-create-source-textarea"
                    value={selectedDbms === 'postgresql' ? dataState.draftValue.postgresql : dataState.draftValue.oracle}
                    onChange={(event) => dataState.setDraftValue((current) => (
                      selectedDbms === 'postgresql'
                        ? { ...current, postgresql: event.target.value }
                        : { ...current, oracle: event.target.value }
                    ))}
                    placeholder={selectedDbms === 'postgresql' ? 'PostgreSQL Data SQL' : 'Oracle Data SQL'}
                  />
                </div>
              </div>
            ) : undefined
          }
          conditionContent={
            conditionState.isEditing ? (
              <textarea className="text-field problem-create-inline-textarea" value={conditionState.draftValue} onChange={(event) => conditionState.setDraftValue(event.target.value)} placeholder="조건" />
            ) : isConditionMissing ? (
              <></>
            ) : undefined
          }
          outputContent={
            outputState.isEditing ? (
              <textarea className="text-field problem-create-inline-textarea" value={outputState.draftValue} onChange={(event) => outputState.setDraftValue(event.target.value)} placeholder="출력" />
            ) : isOutputMissing ? (
              <></>
            ) : undefined
          }
          outputSampleBeforeContent={
            outputSampleState.isEditing ? (
              <div className="problem-create-output-sample-editor">
                <textarea className="text-field problem-create-inline-textarea" value={outputSampleState.draftValue} onChange={(event) => outputSampleState.setDraftValue(event.target.value)} placeholder="출력 예시" />
              </div>
            ) : undefined
          }
        />

        <section ref={answerSectionRef} className={`solve-detail-section problem-create-answer-surface ${buildSectionClassName(isAnswerMissing)}`.trim()}>
          <div className="solve-detail-section-header">
            <div className="solve-detail-section-title-row">
              <h2 className="solve-detail-section-title">정답 SQL</h2>
              <InlineEditActions isEditing={answerState.isEditing} onEdit={answerState.startEditing} onCancel={answerState.cancelEditing} onConfirm={answerState.confirmEditing} />
            </div>
          </div>
          <div className="solve-detail-section-divider">
            <span className="solve-detail-section-divider-line" />
          </div>
          <div className="solve-detail-section-body">
            {answerState.isEditing ? (
              <textarea className="text-field problem-create-answer-textarea" value={answerState.draftValue} onChange={(event) => answerState.setDraftValue(event.target.value)} placeholder="정답 SQL" />
            ) : (
              <pre className="problem-create-answer-preview">
                {answerState.appliedValue.trim() !== '' ? answerState.appliedValue : <span className="problem-create-placeholder-text">정답 SQL</span>}
              </pre>
            )}
          </div>
        </section>

        <section className="solve-detail-section problem-create-editor-surface">
          <div className="solve-detail-section-header">
            <div className="solve-detail-section-title-row">
              <h2 className="solve-detail-section-title">에디터</h2>
            </div>
          </div>
          <div className="solve-detail-section-divider">
            <span className="solve-detail-section-divider-line" />
          </div>
          <div className="solve-detail-section-body">
            <textarea className="text-field problem-create-inline-textarea" value={editorSql} onChange={(event) => setEditorSql(event.target.value)} placeholder="자동완성 확인용 SQL을 입력해라." />
          </div>
        </section>
      </div>

      <button type="button" className="btn primary problem-create-floating-submit-button" onClick={handleCreateProblem}>
        문제 저장
      </button>

      <StatusPopup open={popupState.open} level={popupState.level} message={popupState.message} onConfirm={() => setPopupState((current) => ({ ...current, open: false }))} />
    </>
  );
}

export default function ProblemCreatePage() {
  return <ProblemCreateContent />;
}

function collectMissingFields(values: {
  title: string;
  description: string;
  condition: string;
  output: string;
  outputSample: string;
  answer: string;
  ddlPostgresql: string;
  ddlOracle: string;
  dataPostgresql: string;
  dataOracle: string;
}) {
  const missingFields: MissingField[] = [];

  if (values.title.trim() === '') missingFields.push({ key: 'title', label: '문제 제목' });
  if (values.description.trim() === '') missingFields.push({ key: 'description', label: '설명' });
  if (values.condition.trim() === '') missingFields.push({ key: 'condition', label: '조건' });
  if (values.output.trim() === '') missingFields.push({ key: 'output', label: '출력' });
  if (values.outputSample.trim() === '') missingFields.push({ key: 'outputSample', label: '출력 예시' });
  if (values.answer.trim() === '') missingFields.push({ key: 'answer', label: '정답 SQL' });
  if (values.ddlPostgresql.trim() === '' && values.ddlOracle.trim() === '') missingFields.push({ key: 'tableInfo', label: '테이블 정보' });
  if (values.dataPostgresql.trim() === '' && values.dataOracle.trim() === '') missingFields.push({ key: 'dataSample', label: '데이터 예시' });

  return missingFields;
}
