import { useEffect, useRef, useState, type MouseEvent as ReactMouseEvent, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import ProblemDetailContent from '../components/problem/ProblemDetailContent';
import { fetchProblemDetail, type ProblemDetailData } from '../lib/problemApi';
import { mockProblemDetailById, mockProblemDetails } from '../mocks/problemDetail';
import type { DbmsType, MockResult, ProblemDetail, RuntimeDistribution } from '../types/domain';

interface ProblemSolvePageProps {
  problemId: string;
}

type PanelKey = 'editor' | 'submit';

interface PanelVisibilityState {
  editor: boolean;
  submit: boolean;
}

interface PanelDetachState {
  editor: boolean;
  submit: boolean;
}

interface WorkspaceDragState {
  leftKey: PanelKey;
  rightKey: PanelKey;
  startX: number;
  startLeftWeight: number;
  startRightWeight: number;
}

interface FloatingPanelLayout {
  left: number;
  top: number;
  width: number;
  height: number;
}

interface FloatingPanelLayoutState {
  editor: FloatingPanelLayout;
  submit: FloatingPanelLayout;
}

interface FloatingMoveState {
  panelKey: PanelKey;
  startX: number;
  startY: number;
  startLeft: number;
  startTop: number;
}

interface FloatingResizeState {
  panelKey: PanelKey;
  startX: number;
  startY: number;
  startWidth: number;
  startHeight: number;
}

interface ExternalWindowState {
  editor: boolean;
  submit: boolean;
}

interface PanelExternalWindowProps {
  panelKey: PanelKey;
  title: string;
  layout: FloatingPanelLayout;
  onClose: () => void;
  children: ReactNode;
}

const panelOrder: PanelKey[] = ['editor', 'submit'];

const panelLabels: Record<PanelKey, string> = {
  editor: 'SQL Editor',
  submit: '제출 결과',
};

const panelMinWidths: Record<PanelKey, number> = {
  editor: 420,
  submit: 340,
};

const panelMinHeights: Record<PanelKey, number> = {
  editor: 320,
  submit: 260,
};

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function getDbmsLabel(dbms: DbmsType) {
  return dbms === 'postgresql' ? 'PostgreSQL' : 'Oracle';
}

function getAvailableDbms(problem: ProblemDetail) {
  return problem.dbmsOptions.filter((dbms) => !problem.disabledDbms.includes(dbms));
}

function getRuntimeDistribution(problem: ProblemDetail, selectedDbms: DbmsType): RuntimeDistribution | undefined {
  return problem.runtimeDistributions?.[selectedDbms] ?? problem.runtimeDistribution;
}

function formatMs(value?: number) {
  if (value === undefined) {
    return '-';
  }

  return `${Math.round(value * 10) / 10}ms`;
}

function toProblemSequence(problemId: string) {
  const [, problemSequence] = problemId.split('-');
  const parsedNumber = Number.parseInt(problemSequence ?? '', 10);

  return Number.isNaN(parsedNumber) ? 0 : parsedNumber;
}

function createFallbackProblemDetail(problemId: string): ProblemDetail {
  const matchedProblem = mockProblemDetailById[problemId];
  if (matchedProblem) {
    return {
      ...matchedProblem,
      problemNumber: matchedProblem.problemNumber ?? problemId,
    };
  }

  return {
    ...mockProblemDetails[0],
    id: problemId,
    number: toProblemSequence(problemId),
    problemNumber: problemId,
    title: '',
    preview: '',
    description: '',
  };
}

function getTopPercent(values: number[], currentValue: number) {
  if (values.length === 0) {
    return null;
  }

  const betterCount = values.filter((value) => value < currentValue).length;
  return Math.max(1, Math.round(((betterCount + 1) / (values.length + 1)) * 100));
}

function createInitialFloatingLayouts(): FloatingPanelLayoutState {
  return {
    editor: {
      left: 24,
      top: 118,
      width: 760,
      height: 620,
    },
    submit: {
      left: 812,
      top: 118,
      width: 400,
      height: 330,
    },
  };
}

function formatCellValue(value: string | number | boolean | null | undefined) {
  if (value == null) {
    return '';
  }

  return String(value);
}

function createResultColumnLabels(result: MockResult) {
  const columnCount = result.rows.reduce((maxCount, row) => Math.max(maxCount, row.columns.length), 0);
  return Array.from({ length: columnCount }, (_, index) => `컬럼 ${index + 1}`);
}

function renderResultTable(result: MockResult | null, emptyMessage: string) {
  if (result == null || result.rows.length === 0) {
    return <div className="solve-result-empty solve-result-empty-table">{emptyMessage}</div>;
  }

  const columnLabels = createResultColumnLabels(result);

  return (
    <div className="result-table-scroll">
      <table className="result-table solve-pane-result-table">
        <thead>
          <tr>
            {columnLabels.map((columnLabel) => (
              <th key={columnLabel} scope="col">
                {columnLabel}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {result.rows.map((row, rowIndex) => (
            <tr key={`result-row-${rowIndex}`}>
              {columnLabels.map((columnLabel, columnIndex) => (
                <td key={`result-row-${rowIndex}-${columnLabel}`}>{formatCellValue(row.columns[columnIndex])}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function copyDocumentStyles(targetDocument: Document) {
  targetDocument.head.innerHTML = '';

  document.querySelectorAll('style, link[rel="stylesheet"]').forEach((styleNode) => {
    targetDocument.head.appendChild(styleNode.cloneNode(true));
  });
}

function ExternalWindowIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M3 3.5h5v1H4V12h7.5V8h1v4A1.5 1.5 0 0 1 11 13.5H4A1.5 1.5 0 0 1 2.5 12V5A1.5 1.5 0 0 1 4 3.5Zm5.5-1h5v5h-1V4.2L8.6 7.6l-.7-.7 3.4-3.4H8.5v-1Z"
        fill="currentColor"
      />
    </svg>
  );
}

function PipIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M2.5 3A1.5 1.5 0 0 1 4.0 1.5h8A1.5 1.5 0 0 1 13.5 3v6A1.5 1.5 0 0 1 12 10.5H8.5v2H12v1H4v-1h3.5v-2H4A1.5 1.5 0 0 1 2.5 9V3Zm1 0V9a.5.5 0 0 0 .5.5h8A.5.5 0 0 0 12.5 9V3a.5.5 0 0 0-.5-.5H4a.5.5 0 0 0-.5.5Zm5.5 1.5h2.5V7H9V4.5Z"
        fill="currentColor"
      />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M4.2 4.2a.75.75 0 0 1 1.06 0L8 6.94l2.74-2.74a.75.75 0 1 1 1.06 1.06L9.06 8l2.74 2.74a.75.75 0 1 1-1.06 1.06L8 9.06 5.26 11.8a.75.75 0 0 1-1.06-1.06L6.94 8 4.2 5.26a.75.75 0 0 1 0-1.06Z"
        fill="currentColor"
      />
    </svg>
  );
}

function PanelExternalWindow({ panelKey, title, layout, onClose, children }: PanelExternalWindowProps) {
  const externalWindowRef = useRef<Window | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const onCloseRef = useRef(onClose);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    const openedWindow = window.open(
      '',
      `quertimizer-${panelKey}`,
      `popup=yes,width=${Math.round(layout.width)},height=${Math.round(layout.height)},left=${Math.round(layout.left)},top=${Math.round(layout.top)}`,
    );

    if (!openedWindow) {
      onCloseRef.current();
      return;
    }

    externalWindowRef.current = openedWindow;
    copyDocumentStyles(openedWindow.document);
    openedWindow.document.title = title;
    openedWindow.document.body.innerHTML = '';
    openedWindow.document.body.className = document.body.className;
    openedWindow.document.body.style.margin = '0';
    openedWindow.document.body.style.background = '#eef3f9';
    openedWindow.document.body.style.overflow = 'hidden';

    const container = openedWindow.document.createElement('div');
    container.className = 'solve-external-window-root';
    openedWindow.document.body.appendChild(container);
    containerRef.current = container;
    setIsReady(true);

    const handleBeforeUnload = () => {
      onCloseRef.current();
    };

    openedWindow.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      openedWindow.removeEventListener('beforeunload', handleBeforeUnload);

      if (!openedWindow.closed) {
        openedWindow.close();
      }
    };
  }, [layout.height, layout.left, layout.top, layout.width, panelKey, title]);

  useEffect(() => {
    if (!externalWindowRef.current || externalWindowRef.current.closed) {
      return;
    }

    externalWindowRef.current.moveTo(Math.round(layout.left), Math.round(layout.top));
    externalWindowRef.current.resizeTo(Math.round(layout.width), Math.round(layout.height));
  }, [layout.height, layout.left, layout.top, layout.width]);

  if (!isReady || !containerRef.current) {
    return null;
  }

  return createPortal(children, containerRef.current);
}

export default function ProblemSolvePage({ problemId }: ProblemSolvePageProps) {
  const workspaceRef = useRef<HTMLDivElement | null>(null);
  const fallbackProblem = createFallbackProblemDetail(problemId);
  const [problemDetail, setProblemDetail] = useState<ProblemDetailData | null>(null);
  const [problemLoadError, setProblemLoadError] = useState<string | null>(null);
  const [executionResult, setExecutionResult] = useState<MockResult | null>(null);
  const [isExecutionSheetOpen, setIsExecutionSheetOpen] = useState(false);
  const [submitResult, setSubmitResult] = useState<MockResult | null>(null);
  const [panelVisibility, setPanelVisibility] = useState<PanelVisibilityState>({
    editor: true,
    submit: true,
  });
  const [detachedPanels, setDetachedPanels] = useState<PanelDetachState>({
    editor: false,
    submit: false,
  });
  const [externalWindowPanels, setExternalWindowPanels] = useState<ExternalWindowState>({
    editor: false,
    submit: false,
  });
  const [panelWeights, setPanelWeights] = useState<Record<PanelKey, number>>({
    editor: 65,
    submit: 35,
  });
  const [workspaceDragState, setWorkspaceDragState] = useState<WorkspaceDragState | null>(null);
  const [floatingLayouts, setFloatingLayouts] = useState<FloatingPanelLayoutState>(() => createInitialFloatingLayouts());
  const [floatingMoveState, setFloatingMoveState] = useState<FloatingMoveState | null>(null);
  const [floatingResizeState, setFloatingResizeState] = useState<FloatingResizeState | null>(null);
  const problem = fallbackProblem;
  const availableDbms = getAvailableDbms(problem);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>(availableDbms[0] ?? problem.dbmsOptions[0] ?? 'postgresql');
  const [sql, setSql] = useState(problem.starterSql);

  const runtimeDistribution = getRuntimeDistribution(problem, selectedDbms);
  const displayProblemNumber = problemDetail?.problemId ?? problem.problemNumber ?? problemId;
  const displayProblemTitle =
    problemDetail?.title ?? (problem.title || (problemLoadError ? '문제 정보를 불러오지 못했다.' : '문제 정보를 불러오는 중...'));
  const submitTimePercent =
    submitResult && runtimeDistribution ? getTopPercent(runtimeDistribution.samples.map((sample) => sample.timeMs), submitResult.executionTimeMs) : null;

  const visibleWorkspacePanels = panelOrder.filter(
    (panelKey) => panelVisibility[panelKey] && !detachedPanels[panelKey] && !externalWindowPanels[panelKey],
  );
  const visibleFloatingPanels = panelOrder.filter(
    (panelKey) => panelVisibility[panelKey] && detachedPanels[panelKey] && !externalWindowPanels[panelKey],
  );
  const visibleExternalWindows = panelOrder.filter((panelKey) => panelVisibility[panelKey] && externalWindowPanels[panelKey]);

  useEffect(() => {
    let cancelled = false;

    setProblemDetail(null);
    setProblemLoadError(null);

    void fetchProblemDetail(problemId)
      .then((detail) => {
        if (cancelled) {
          return;
        }

        setProblemDetail(detail);
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }

        setProblemLoadError(error instanceof Error ? error.message : '문제 상세 조회에 실패했다.');
      });

    return () => {
      cancelled = true;
    };
  }, [problemId]);

  useEffect(() => {
    setSql(fallbackProblem.starterSql);
    setExecutionResult(null);
    setIsExecutionSheetOpen(false);
    setSubmitResult(null);
    setSelectedDbms(availableDbms[0] ?? problem.dbmsOptions[0] ?? 'postgresql');
  }, [availableDbms, fallbackProblem.starterSql, problem.dbmsOptions, problemId]);

  useEffect(() => {
    if (!workspaceDragState) {
      return;
    }

    const handleMove = (event: MouseEvent) => {
      if (!workspaceRef.current) {
        return;
      }

      const rect = workspaceRef.current.getBoundingClientRect();
      const deltaPercent = ((event.clientX - workspaceDragState.startX) / rect.width) * 100;
      const pairTotal = workspaceDragState.startLeftWeight + workspaceDragState.startRightWeight;
      const leftMinPercent = (panelMinWidths[workspaceDragState.leftKey] / rect.width) * 100;
      const rightMinPercent = (panelMinWidths[workspaceDragState.rightKey] / rect.width) * 100;
      const nextLeftWeight = clamp(
        workspaceDragState.startLeftWeight + deltaPercent,
        leftMinPercent,
        pairTotal - rightMinPercent,
      );

      setPanelWeights((current) => ({
        ...current,
        [workspaceDragState.leftKey]: nextLeftWeight,
        [workspaceDragState.rightKey]: pairTotal - nextLeftWeight,
      }));
    };

    const handleUp = () => {
      setWorkspaceDragState(null);
    };

    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);

    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [workspaceDragState]);

  useEffect(() => {
    if (!floatingMoveState) {
      return;
    }

    const handleMove = (event: MouseEvent) => {
      const layout = floatingLayouts[floatingMoveState.panelKey];
      const viewportPadding = 12;
      const nextLeft = clamp(
        floatingMoveState.startLeft + (event.clientX - floatingMoveState.startX),
        viewportPadding,
        window.innerWidth - layout.width - viewportPadding,
      );
      const nextTop = clamp(
        floatingMoveState.startTop + (event.clientY - floatingMoveState.startY),
        viewportPadding,
        window.innerHeight - layout.height - viewportPadding,
      );

      setFloatingLayouts((current) => ({
        ...current,
        [floatingMoveState.panelKey]: {
          ...current[floatingMoveState.panelKey],
          left: nextLeft,
          top: nextTop,
        },
      }));
    };

    const handleUp = () => {
      setFloatingMoveState(null);
    };

    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);

    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [floatingLayouts, floatingMoveState]);

  useEffect(() => {
    if (!floatingResizeState) {
      return;
    }

    const handleMove = (event: MouseEvent) => {
      const currentLayout = floatingLayouts[floatingResizeState.panelKey];
      const viewportPadding = 12;
      const nextWidth = clamp(
        floatingResizeState.startWidth + (event.clientX - floatingResizeState.startX),
        panelMinWidths[floatingResizeState.panelKey],
        window.innerWidth - currentLayout.left - viewportPadding,
      );
      const nextHeight = clamp(
        floatingResizeState.startHeight + (event.clientY - floatingResizeState.startY),
        panelMinHeights[floatingResizeState.panelKey],
        window.innerHeight - currentLayout.top - viewportPadding,
      );

      setFloatingLayouts((current) => ({
        ...current,
        [floatingResizeState.panelKey]: {
          ...current[floatingResizeState.panelKey],
          width: nextWidth,
          height: nextHeight,
        },
      }));
    };

    const handleUp = () => {
      setFloatingResizeState(null);
    };

    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);

    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [floatingLayouts, floatingResizeState]);

  const handleExecute = () => {
    setExecutionResult(problem.mockResult);
    setIsExecutionSheetOpen(true);
  };

  const handleSubmit = () => {
    setSubmitResult(problem.mockResult);
    setPanelVisibility((current) => ({
      ...current,
      submit: true,
    }));
  };

  const togglePanelVisibility = (panelKey: PanelKey) => {
    const nextVisible = !panelVisibility[panelKey];

    if (!nextVisible && detachedPanels[panelKey]) {
      setDetachedPanels((current) => ({
        ...current,
        [panelKey]: false,
      }));
    }

    if (!nextVisible && externalWindowPanels[panelKey]) {
      setExternalWindowPanels((current) => ({
        ...current,
        [panelKey]: false,
      }));
    }

    setPanelVisibility((current) => ({
      ...current,
      [panelKey]: nextVisible,
    }));
  };

  const togglePanelDetach = (panelKey: PanelKey) => {
    if (!panelVisibility[panelKey]) {
      setPanelVisibility((current) => ({
        ...current,
        [panelKey]: true,
      }));
    }

    if (externalWindowPanels[panelKey]) {
      setExternalWindowPanels((current) => ({
        ...current,
        [panelKey]: false,
      }));
    }

    setDetachedPanels((current) => ({
      ...current,
      [panelKey]: !current[panelKey],
    }));
  };

  const togglePanelExternalWindow = (panelKey: PanelKey) => {
    if (!panelVisibility[panelKey]) {
      setPanelVisibility((current) => ({
        ...current,
        [panelKey]: true,
      }));
    }

    if (detachedPanels[panelKey]) {
      setDetachedPanels((current) => ({
        ...current,
        [panelKey]: false,
      }));
    }

    setExternalWindowPanels((current) => ({
      ...current,
      [panelKey]: !current[panelKey],
    }));
  };

  const startFloatingMove = (panelKey: PanelKey, event: ReactMouseEvent<HTMLElement>) => {
    if (event.button !== 0) {
      return;
    }

    const target = event.target as HTMLElement;
    if (target.closest('button')) {
      return;
    }

    event.preventDefault();
    setFloatingMoveState({
      panelKey,
      startX: event.clientX,
      startY: event.clientY,
      startLeft: floatingLayouts[panelKey].left,
      startTop: floatingLayouts[panelKey].top,
    });
  };

  const startFloatingResize = (panelKey: PanelKey, event: ReactMouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    setFloatingResizeState({
      panelKey,
      startX: event.clientX,
      startY: event.clientY,
      startWidth: floatingLayouts[panelKey].width,
      startHeight: floatingLayouts[panelKey].height,
    });
  };

  const renderPanelActions = (panelKey: PanelKey) => (
    <div className="solve-pane-actions">
      <button
        type="button"
        className={`mini-toggle solve-pane-action solve-pane-action-icon ${externalWindowPanels[panelKey] ? 'is-selected' : ''}`}
        aria-label={externalWindowPanels[panelKey] ? `${panelLabels[panelKey]} 새 창 닫기` : `${panelLabels[panelKey]} 새 창으로 열기`}
        onClick={() => togglePanelExternalWindow(panelKey)}
      >
        <ExternalWindowIcon />
      </button>
      <button
        type="button"
        className={`mini-toggle solve-pane-action solve-pane-action-icon ${detachedPanels[panelKey] ? 'is-selected' : ''}`}
        aria-label={detachedPanels[panelKey] ? `${panelLabels[panelKey]} 다시 붙이기` : `${panelLabels[panelKey]} PIP 열기`}
        onClick={() => togglePanelDetach(panelKey)}
      >
        <PipIcon />
      </button>
      <button
        type="button"
        className="mini-toggle solve-pane-action solve-pane-action-icon"
        aria-label={`${panelLabels[panelKey]} 닫기`}
        onClick={() => togglePanelVisibility(panelKey)}
      >
        <CloseIcon />
      </button>
    </div>
  );

  const renderPanelHeader = (panelKey: PanelKey, isFloating: boolean) => (
    <div
      className={`solve-pane-header ${isFloating ? 'is-draggable' : ''}`}
      onMouseDown={isFloating ? (event) => startFloatingMove(panelKey, event) : undefined}
    >
      <h2 className="panel-title solve-pane-title">{panelLabels[panelKey]}</h2>
      {renderPanelActions(panelKey)}
    </div>
  );

  const renderExecutionSheet = () => {
    if (!executionResult || !isExecutionSheetOpen) {
      return null;
    }

    return (
      <section className="solve-editor-result-sheet is-open">
        <div className="solve-editor-result-sheet-header">
          <strong className="solve-editor-result-sheet-title">실행 결과</strong>
          <button
            type="button"
            className="mini-toggle solve-editor-result-sheet-close"
            aria-label="실행 결과 닫기"
            onClick={() => setIsExecutionSheetOpen(false)}
          >
            <CloseIcon />
          </button>
        </div>

        <div className="solve-pane-result-stack">
          <div className="solve-result-lines">
            <div className="solve-result-line">
              <span className="solve-result-label">실행시간</span>
              <strong className="solve-result-value">{formatMs(executionResult.executionTimeMs)}</strong>
            </div>
            <div className="solve-result-line">
              <span className="solve-result-label">반환 행 수</span>
              <strong className="solve-result-value">{executionResult.rows.length}</strong>
            </div>
          </div>

          {executionResult.message ? <p className="solve-pane-result-message">{executionResult.message}</p> : null}
          {renderResultTable(executionResult, '표시할 실행 결과가 없다.')}
        </div>
      </section>
    );
  };

  const renderEditorPanel = (isFloating: boolean) => (
    <section className={`panel-card solve-pane solve-pane-editor ${isFloating ? 'is-floating' : ''}`}>
      {renderPanelHeader('editor', isFloating)}

      <div className="solve-editor-surface">
        <div className="solve-editor-surface-header">
          <div className="solve-editor-surface-meta">
            <span className="solve-editor-file">main.sql</span>
            <span className="subtle-chip inverted">{getDbmsLabel(selectedDbms)}</span>
          </div>
          <div className="solve-editor-actions">
            <button type="button" className="btn ghost" onClick={() => setSql(problem.starterSql)}>
              초기화
            </button>
            <button type="button" className="btn secondary" onClick={handleExecute} disabled={sql.trim().length === 0}>
              실행
            </button>
            <button type="button" className="btn primary" onClick={handleSubmit} disabled={sql.trim().length === 0}>
              제출
            </button>
          </div>
        </div>

        <div className="solve-editor-surface-body">
          <textarea
            className="solve-sql-editor"
            spellCheck={false}
            value={sql}
            onChange={(event) => setSql(event.target.value)}
            aria-label="SQL Editor"
          />
          {renderExecutionSheet()}
        </div>
      </div>
    </section>
  );

  const renderSubmitPanel = (isFloating: boolean) => (
    <section className={`panel-card solve-pane ${isFloating ? 'is-floating' : ''}`}>
      {renderPanelHeader('submit', isFloating)}

      {submitResult ? (
        <div className="solve-pane-result-stack">
          <div className="solve-result-lines">
            <div className="solve-result-line">
              <span className="solve-result-label">판정</span>
              <strong className="solve-result-value">{submitResult.status === 'success' ? '정답' : '확인 필요'}</strong>
            </div>
            <div className="solve-result-line">
              <span className="solve-result-label">실행시간</span>
              <strong className="solve-result-value">{formatMs(submitResult.executionTimeMs)}</strong>
            </div>
          </div>

          {submitResult.message ? <p className="solve-pane-result-message">{submitResult.message}</p> : null}

          <div className="solve-performance-card">
            <p className="solve-performance-title">성능 비교</p>
            <div className="solve-performance-list">
              <div className="solve-performance-item">
                <span>이 문제 최소 실행시간</span>
                <strong>{formatMs(runtimeDistribution?.fastestTimeMs)}</strong>
              </div>
              <div className="solve-performance-item">
                <span>이 문제 평균 실행시간</span>
                <strong>{formatMs(runtimeDistribution?.averageTimeMs)}</strong>
              </div>
              <div className="solve-performance-item">
                <span>현재 속도 구간</span>
                <strong>{submitTimePercent ? `상위 ${submitTimePercent}%` : '비교 데이터 없음'}</strong>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="solve-result-empty">제출 후 결과가 이 영역에 표시된다.</div>
      )}
    </section>
  );

  const renderPanel = (panelKey: PanelKey, isFloating: boolean): ReactNode => {
    if (panelKey === 'editor') {
      return renderEditorPanel(isFloating);
    }

    return renderSubmitPanel(isFloating);
  };

  const renderSplitter = (leftKey: PanelKey, rightKey: PanelKey) => (
    <button
      type="button"
      className="solve-pane-splitter"
      aria-label={`${panelLabels[leftKey]}와 ${panelLabels[rightKey]} 너비 조절`}
      onMouseDown={(event) => {
        event.preventDefault();
        setWorkspaceDragState({
          leftKey,
          rightKey,
          startX: event.clientX,
          startLeftWeight: panelWeights[leftKey],
          startRightWeight: panelWeights[rightKey],
        });
      }}
    >
      <span />
    </button>
  );

  return (
    <div className="page-stack">
      <div className="solve-page-topbar solve-page-topbar-dbms">
        <div className="solve-workspace-toolbar-group">
          {availableDbms.map((dbms) => (
            <button
              key={dbms}
              type="button"
              className={`mini-toggle solve-dbms-button ${selectedDbms === dbms ? 'is-selected' : ''}`}
              onClick={() => setSelectedDbms(dbms)}
            >
              {getDbmsLabel(dbms)}
            </button>
          ))}
        </div>
      </div>

      <section className="panel-card solve-page-hero">
        <div className="solve-page-hero-copy solve-page-hero-copy-wide">
          <div className="solve-title-row">
            <span className="solve-problem-number">문제 {displayProblemNumber}</span>
            <h1 className="solve-problem-title">{displayProblemTitle}</h1>
          </div>

          {problemLoadError ? <p className="content-text solve-problem-description">{problemLoadError}</p> : null}
          {!problemLoadError && problemDetail == null ? (
            <p className="content-text solve-problem-description">문제 상세를 불러오는 중...</p>
          ) : null}
          {problemDetail ? <ProblemDetailContent detail={problemDetail} /> : null}
        </div>
      </section>

      <section className="panel-card solve-workspace-card">
        <div className="solve-workspace-toolbar">
          <div className="solve-workspace-panel-tabs">
            {panelOrder.map((panelKey) => (
              <button
                key={panelKey}
                type="button"
                className={`mini-toggle solve-workspace-panel-tab ${panelVisibility[panelKey] ? 'is-selected' : ''} ${panelVisibility[panelKey] && detachedPanels[panelKey] ? 'is-detached' : ''}`}
                aria-pressed={panelVisibility[panelKey]}
                onClick={() => togglePanelVisibility(panelKey)}
              >
                <span>{panelLabels[panelKey]}</span>
                {panelVisibility[panelKey] && detachedPanels[panelKey] ? <span className="solve-panel-state-chip">PIP</span> : null}
              </button>
            ))}
          </div>
        </div>

        <div className="solve-workspace" ref={workspaceRef}>
          {visibleWorkspacePanels.length === 0 ? (
            <div className="solve-workspace-empty">탭을 눌러 작업영역을 다시 열어라.</div>
          ) : (
            visibleWorkspacePanels.map((panelKey, index) => (
              <div key={panelKey} className="solve-workspace-segment" style={{ flex: `${panelWeights[panelKey]} 1 0` }}>
                {renderPanel(panelKey, false)}
                {index < visibleWorkspacePanels.length - 1 ? renderSplitter(panelKey, visibleWorkspacePanels[index + 1]) : null}
              </div>
            ))
          )}
        </div>
      </section>

      {visibleFloatingPanels.map((panelKey) => (
        <div
          key={panelKey}
          className="solve-floating-pane-shell"
          style={{
            left: `${floatingLayouts[panelKey].left}px`,
            top: `${floatingLayouts[panelKey].top}px`,
            width: `${floatingLayouts[panelKey].width}px`,
            height: `${floatingLayouts[panelKey].height}px`,
          }}
        >
          {renderPanel(panelKey, true)}
          <button
            type="button"
            className="solve-floating-pane-resize"
            aria-label={`${panelLabels[panelKey]} 크기 조절`}
            onMouseDown={(event) => startFloatingResize(panelKey, event)}
          >
            <span aria-hidden="true" />
          </button>
        </div>
      ))}

      {visibleExternalWindows.map((panelKey) => (
        <PanelExternalWindow
          key={`external-${panelKey}`}
          panelKey={panelKey}
          title={`Quertimizer - ${panelLabels[panelKey]}`}
          layout={floatingLayouts[panelKey]}
          onClose={() =>
            setExternalWindowPanels((current) => ({
              ...current,
              [panelKey]: false,
            }))
          }
        >
          <div className="solve-external-window-root-inner">{renderPanel(panelKey, false)}</div>
        </PanelExternalWindow>
      ))}
    </div>
  );
}
