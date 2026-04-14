import { useEffect, useMemo, useRef, useState, type ChangeEvent, type Dispatch, type SetStateAction } from 'react';
import StatusPopup from '../components/common/StatusPopup';
import ProblemDetailContent from '../components/problem/ProblemDetailContent';
import {
  createProblem,
  fetchProblemSetDetail,
  fetchProblemSets,
  type ProblemDetailData,
  type ProblemSetDetailData,
  type ProblemSetSummary,
} from '../lib/problemApi';
import { navigate } from '../lib/navigation';
import './ProblemCreatePage.css';

type ProblemSetMode = 'existing' | 'new';

interface EditableDraftState<T> {
  appliedValue: T;
  draftValue: T;
  isEditing: boolean;
  setDraftValue: Dispatch<SetStateAction<T>>;
  startEditing: () => void;
  cancelEditing: () => void;
  confirmEditing: () => void;
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

  return {
    appliedValue,
    draftValue,
    isEditing,
    setDraftValue,
    startEditing,
    cancelEditing,
    confirmEditing,
  };
}

function getProblemSetLabel(problemSetId: string) {
  return `\uD14C\uC774\uBE14\uC14B ${problemSetId}`;
}

function getProblemNumberLabel(problemSetMode: ProblemSetMode, problemSetId: string | null) {
  if (problemSetMode === 'existing' && problemSetId) {
    return `${problemSetId} - X`;
  }

  return '\uC2E0\uADDC - X';
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
      <button type="button" className={`solve-detail-section-action problem-create-edit-action ${isEditing ? 'is-active' : ''}`.trim()} aria-label={isEditing ? '\uC218\uC815 \uCDE8\uC18C' : '\uC218\uC815'} onClick={isEditing ? onCancel : onEdit}>
        {isEditing ? <CloseIcon /> : <EditIcon />}
      </button>
      {isEditing ? (
        <button type="button" className="solve-detail-section-action problem-create-confirm-action" aria-label="\uC218\uC815 \uC801\uC6A9" onClick={onConfirm}>
          <CheckIcon />
        </button>
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
  const [problemSets, setProblemSets] = useState<ProblemSetSummary[]>([]);
  const [selectedProblemSetId, setSelectedProblemSetId] = useState<string | null>(null);
  const [loadedProblemSetDetail, setLoadedProblemSetDetail] = useState<ProblemSetDetailData>(EMPTY_PROBLEM_SET_DETAIL);
  const [isProblemSetLoading, setIsProblemSetLoading] = useState(false);
  const [problemSetErrorMessage, setProblemSetErrorMessage] = useState('');
  const [includedTableNames, setIncludedTableNames] = useState<string[]>([]);
  const [editorSql, setEditorSql] = useState('');
  const [popupState, setPopupState] = useState<PopupState>({ open: false, level: 2, message: '' });

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
          setProblemSetErrorMessage('\uD14C\uC774\uBE14\uC14B \uBAA9\uB85D\uC744 \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uB2E4.');
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
      return;
    }

    const targetProblemSetId = selectedProblemSetId;
    let cancelled = false;

    async function loadProblemSetDetail() {
      setIsProblemSetLoading(true);
      setProblemSetErrorMessage('');

      try {
        const nextDetail = await fetchProblemSetDetail(targetProblemSetId);
        if (cancelled) return;

        setLoadedProblemSetDetail(nextDetail);
      } catch {
        if (!cancelled) {
          setLoadedProblemSetDetail(EMPTY_PROBLEM_SET_DETAIL);
          setProblemSetErrorMessage('\uD14C\uC774\uBE14\uC14B \uC0C1\uC138 \uC815\uBCF4\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uB2E4.');
        }
      } finally {
        if (!cancelled) {
          setIsProblemSetLoading(false);
        }
      }
    }

    loadProblemSetDetail();
    return () => {
      cancelled = true;
    };
  }, [problemSetMode, selectedProblemSetId]);

  const currentProblemSetDetail = problemSetMode === 'existing'
    ? loadedProblemSetDetail
    : {
        problemSetId: '',
        ddlPostgresql: ddlState.appliedValue.postgresql,
        ddlOracle: ddlState.appliedValue.oracle,
        dataPostgresql: dataState.appliedValue.postgresql,
        dataOracle: dataState.appliedValue.oracle,
      };
  const selectedProblemSetValue = problemSetMode === 'existing' && selectedProblemSetId != null
    ? selectedProblemSetId
    : NEW_PROBLEM_SET_OPTION_VALUE;
  const previewProblemSetId = problemSetMode === 'existing' && selectedProblemSetId != null
    ? selectedProblemSetId
    : '\uC2E0\uADDC';

  const availableTableNames = useMemo(() => {
    const fallbackDdl = currentProblemSetDetail.ddlPostgresql.trim() !== ''
      ? currentProblemSetDetail.ddlPostgresql
      : currentProblemSetDetail.ddlOracle;

    return getTableNamesFromDdl(fallbackDdl);
  }, [currentProblemSetDetail.ddlOracle, currentProblemSetDetail.ddlPostgresql]);

  useEffect(() => {
    setIncludedTableNames((current) => {
      const filtered = current.filter((tableName) => availableTableNames.includes(tableName));

      if (availableTableNames.length === 0) {
        return [];
      }

      if (filtered.length > 0) {
        return filtered;
      }

      return availableTableNames;
    });
  }, [availableTableNames]);

  const filteredDdlPostgresql = useMemo(() => filterDdlByTableNames(currentProblemSetDetail.ddlPostgresql, includedTableNames), [currentProblemSetDetail.ddlPostgresql, includedTableNames]);
  const filteredDdlOracle = useMemo(() => filterDdlByTableNames(currentProblemSetDetail.ddlOracle, includedTableNames), [currentProblemSetDetail.ddlOracle, includedTableNames]);
  const filteredDataPostgresql = useMemo(() => filterDataSqlByTableNames(currentProblemSetDetail.dataPostgresql, includedTableNames), [currentProblemSetDetail.dataPostgresql, includedTableNames]);
  const filteredDataOracle = useMemo(() => filterDataSqlByTableNames(currentProblemSetDetail.dataOracle, includedTableNames), [currentProblemSetDetail.dataOracle, includedTableNames]);

  const previewDetail = useMemo<ProblemDetailData>(
    () => ({
      problemId: `${previewProblemSetId}-X`,
      title: heroState.appliedValue.title,
      description: heroState.appliedValue.description,
      ddlPostgresql: filteredDdlPostgresql,
      ddlOracle: filteredDdlOracle,
      dataPostgresql: filteredDataPostgresql,
      dataOracle: filteredDataOracle,
      condition: conditionState.appliedValue,
      output: outputState.appliedValue,
      outputSample: outputSampleState.appliedValue,
    }),
    [
      conditionState.appliedValue,
      filteredDataOracle,
      filteredDataPostgresql,
      filteredDdlOracle,
      filteredDdlPostgresql,
      heroState.appliedValue.description,
      heroState.appliedValue.title,
      outputSampleState.appliedValue,
      outputState.appliedValue,
      previewProblemSetId,
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

  function handleProblemSetSelectChange(event: ChangeEvent<HTMLSelectElement>) {
    const nextValue = event.target.value;

    if (nextValue === NEW_PROBLEM_SET_OPTION_VALUE) {
      setProblemSetMode('new');
      setSelectedProblemSetId(null);
      return;
    }

    setProblemSetMode('existing');
    setSelectedProblemSetId(nextValue);
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
      const createdProblemId = await createProblem({
        title: heroState.appliedValue.title.trim(),
        description: heroState.appliedValue.description.trim(),
        ddlPostgresql: filteredDdlPostgresql,
        ddlOracle: filteredDdlOracle,
        condition: conditionState.appliedValue.trim(),
        output: outputState.appliedValue.trim(),
        outputSample: outputSampleState.appliedValue.trim(),
        answer: answerState.appliedValue.trim(),
        problemSetMode,
        problemSetId: problemSetMode === 'existing' ? selectedProblemSetId ?? undefined : undefined,
        dataPostgresql: problemSetMode === 'new' ? filteredDataPostgresql : undefined,
        dataOracle: problemSetMode === 'new' ? filteredDataOracle : undefined,
      });

      navigate(`/problems/${encodeURIComponent(createdProblemId)}`);
    } catch {
      setPopupState({
        open: true,
        level: 2,
        message: '\uBB38\uC81C \uC0DD\uC131\uC5D0 \uC2E4\uD328\uD588\uB2E4.',
      });
    }
  }

  return (
    <>
      <div ref={pageRef} className="page-stack problem-create-page">
        <section ref={heroSectionRef} className="solve-page-hero solve-surface-section problem-create-hero">
          <div className="solve-page-hero-copy solve-page-hero-copy-wide">
            <div className="problem-create-number-row">
              <div className="tooltip-anchor problem-create-problem-number-tooltip">
                <span className="solve-problem-number">{getProblemNumberLabel(problemSetMode, selectedProblemSetId)}</span>
                {problemSetMode === 'existing' && selectedProblemSetId ? (
                  <div className="ui-tooltip is-passive" role="tooltip">
                    <span className="ui-tooltip-caption">{'\uD604\uC7AC \uD14C\uC774\uBE14\uC14B\uC758 \uAC00\uC7A5 \uB9C8\uC9C0\uB9C9 \uBB38\uC81C \uBC88\uD638 + 1\uC774 \uBD80\uC5EC\uB429\uB2C8\uB2E4.'}</span>
                  </div>
                ) : null}
              </div>
              <select className="text-field problem-create-select" value={selectedProblemSetValue} onChange={handleProblemSetSelectChange}>
                <option value={NEW_PROBLEM_SET_OPTION_VALUE}>{'\uC2E0\uADDC \uD14C\uC774\uBE14\uC14B'}</option>
                {problemSets.map((problemSet) => (
                  <option key={problemSet.problemSetId} value={problemSet.problemSetId}>
                    {getProblemSetLabel(problemSet.problemSetId)}
                  </option>
                ))}
              </select>
            </div>

            <div className="problem-create-title-edit-row">
              {heroState.isEditing ? (
                <input className="problem-create-title-input" value={heroState.draftValue.title} onChange={(event) => heroState.setDraftValue((current) => ({ ...current, title: event.target.value }))} placeholder={'\uBB38\uC81C \uC81C\uBAA9'} />
              ) : (
                <h1 className="solve-problem-title">
                  {heroState.appliedValue.title.trim() !== '' ? heroState.appliedValue.title : <span className="problem-create-placeholder-text">{'\uBB38\uC81C \uC81C\uBAA9'}</span>}
                </h1>
              )}
              <InlineEditActions isEditing={heroState.isEditing} onEdit={heroState.startEditing} onCancel={heroState.cancelEditing} onConfirm={heroState.confirmEditing} />
            </div>

            {heroState.isEditing ? (
              <textarea className="text-field problem-create-inline-textarea problem-create-description-editor" value={heroState.draftValue.description} onChange={(event) => heroState.setDraftValue((current) => ({ ...current, description: event.target.value }))} placeholder={'\uC124\uBA85'} />
            ) : (
              <p className="solve-problem-description">
                {heroState.appliedValue.description.trim() !== '' ? heroState.appliedValue.description : <span className="problem-create-placeholder-text">{'\uC124\uBA85'}</span>}
              </p>
            )}
          </div>
        </section>

        <ProblemDetailContent
          detail={previewDetail}
          selectedDbms="postgresql"
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
                  <div className="problem-create-source-grid">
                    <textarea className="text-field problem-create-source-textarea" value={ddlState.draftValue.postgresql} onChange={(event) => ddlState.setDraftValue((current) => ({ ...current, postgresql: event.target.value }))} placeholder="PostgreSQL DDL" />
                    <textarea className="text-field problem-create-source-textarea" value={ddlState.draftValue.oracle} onChange={(event) => ddlState.setDraftValue((current) => ({ ...current, oracle: event.target.value }))} placeholder="Oracle DDL" />
                  </div>
                </div>
              ) : null}

              {problemSetMode === 'existing' && isProblemSetLoading ? <p className="content-text">{'\uD14C\uC774\uBE14\uC14B \uC815\uBCF4\uB97C \uBD88\uB7EC\uC624\uB294 \uC911\uC774\uB2E4.'}</p> : null}
              {problemSetMode === 'existing' && problemSetErrorMessage ? <p className="problem-create-error">{problemSetErrorMessage}</p> : null}

              {availableTableNames.length > 0 ? (
                <div className="problem-create-table-selector">
                  <div className="problem-create-table-chip-row">
                    {availableTableNames.map((tableName) => (
                      <label key={tableName} className="problem-create-table-chip">
                        <input type="checkbox" checked={includedTableNames.includes(tableName)} onChange={() => setIncludedTableNames((current) => (current.includes(tableName) ? current.filter((item) => item !== tableName) : [...current, tableName]))} />
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
                <div className="problem-create-source-grid">
                  <textarea className="text-field problem-create-source-textarea" value={dataState.draftValue.postgresql} onChange={(event) => dataState.setDraftValue((current) => ({ ...current, postgresql: event.target.value }))} placeholder="PostgreSQL Data SQL" />
                  <textarea className="text-field problem-create-source-textarea" value={dataState.draftValue.oracle} onChange={(event) => dataState.setDraftValue((current) => ({ ...current, oracle: event.target.value }))} placeholder="Oracle Data SQL" />
                </div>
              </div>
            ) : undefined
          }
          conditionContent={
            conditionState.isEditing ? (
              <textarea className="text-field problem-create-inline-textarea" value={conditionState.draftValue} onChange={(event) => conditionState.setDraftValue(event.target.value)} placeholder={'\uC870\uAC74'} />
            ) : isConditionMissing ? (
              <></>
            ) : undefined
          }
          outputContent={
            outputState.isEditing ? (
              <textarea className="text-field problem-create-inline-textarea" value={outputState.draftValue} onChange={(event) => outputState.setDraftValue(event.target.value)} placeholder={'\uCD9C\uB825'} />
            ) : isOutputMissing ? (
              <></>
            ) : undefined
          }
          outputSampleBeforeContent={
            outputSampleState.isEditing ? (
              <div className="problem-create-output-sample-editor">
                <textarea className="text-field problem-create-inline-textarea" value={outputSampleState.draftValue} onChange={(event) => outputSampleState.setDraftValue(event.target.value)} placeholder={'\uCD9C\uB825 \uC608\uC2DC'} />
              </div>
            ) : undefined
          }
        />

        <section ref={answerSectionRef} className={`solve-detail-section problem-create-answer-surface ${buildSectionClassName(isAnswerMissing)}`.trim()}>
          <div className="solve-detail-section-header">
            <div className="solve-detail-section-title-row">
              <h2 className="solve-detail-section-title">{'\uC815\uB2F5 SQL'}</h2>
              <InlineEditActions isEditing={answerState.isEditing} onEdit={answerState.startEditing} onCancel={answerState.cancelEditing} onConfirm={answerState.confirmEditing} />
            </div>
          </div>
          <div className="solve-detail-section-divider">
            <span className="solve-detail-section-divider-line" />
          </div>
          <div className="solve-detail-section-body">
            {answerState.isEditing ? (
              <textarea className="text-field problem-create-answer-textarea" value={answerState.draftValue} onChange={(event) => answerState.setDraftValue(event.target.value)} placeholder={'\uC815\uB2F5 SQL'} />
            ) : (
              <pre className="problem-create-answer-preview">
                {answerState.appliedValue.trim() !== '' ? answerState.appliedValue : <span className="problem-create-placeholder-text">{'\uC815\uB2F5 SQL'}</span>}
              </pre>
            )}
          </div>
        </section>

        <section className="solve-detail-section problem-create-editor-surface">
          <div className="solve-detail-section-header">
            <div className="solve-detail-section-title-row">
              <h2 className="solve-detail-section-title">{'\uC5D0\uB514\uD130'}</h2>
            </div>
          </div>
          <div className="solve-detail-section-divider">
            <span className="solve-detail-section-divider-line" />
          </div>
          <div className="solve-detail-section-body">
            <textarea className="text-field problem-create-inline-textarea" value={editorSql} onChange={(event) => setEditorSql(event.target.value)} placeholder={'\uC790\uB3D9\uC644\uC131 \uD655\uC778\uC6A9 SQL\uC744 \uC785\uB825\uD574\uB77C.'} />
          </div>
        </section>
      </div>

      <button type="button" className="btn primary problem-create-floating-submit-button" onClick={handleCreateProblem}>
        {'\uBB38\uC81C \uC0DD\uC131'}
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

  if (values.title.trim() === '') missingFields.push({ key: 'title', label: '\uBB38\uC81C \uC81C\uBAA9' });
  if (values.description.trim() === '') missingFields.push({ key: 'description', label: '\uC124\uBA85' });
  if (values.condition.trim() === '') missingFields.push({ key: 'condition', label: '\uC870\uAC74' });
  if (values.output.trim() === '') missingFields.push({ key: 'output', label: '\uCD9C\uB825' });
  if (values.outputSample.trim() === '') missingFields.push({ key: 'outputSample', label: '\uCD9C\uB825 \uC608\uC2DC' });
  if (values.answer.trim() === '') missingFields.push({ key: 'answer', label: '\uC815\uB2F5 SQL' });
  if (values.ddlPostgresql.trim() === '' && values.ddlOracle.trim() === '') missingFields.push({ key: 'tableInfo', label: '\uD14C\uC774\uBE14 \uC815\uBCF4' });
  if (values.dataPostgresql.trim() === '' && values.dataOracle.trim() === '') missingFields.push({ key: 'dataSample', label: '\uB370\uC774\uD130 \uC608\uC2DC' });

  return missingFields;
}
