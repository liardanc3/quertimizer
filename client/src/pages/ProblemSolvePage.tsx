import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react';
import { PROBLEMS_PATH, navigate } from '../lib/navigation';
import { mockProblemDetailById, mockProblemDetails } from '../mocks/problemDetail';
import type { DbmsType, MockResult, ProblemDetail, RuntimeDistribution } from '../types/domain';

interface ProblemSolvePageProps {
  problemId: string;
}

type PanelKey = 'left' | 'center' | 'right';
type EditorThemeKey = 'slate' | 'paper' | 'forest';
type EditorThemeStyle = CSSProperties &
  Record<'--solve-editor-bg' | '--solve-editor-fg' | '--solve-editor-header' | '--solve-editor-border' | '--solve-editor-font', string>;

interface PanelVisibilityState {
  left: boolean;
  center: boolean;
  right: boolean;
}

interface DragState {
  leftKey: PanelKey;
  rightKey: PanelKey;
  startX: number;
  startLeftWeight: number;
  startRightWeight: number;
}

interface SchemaEntry {
  name: string;
  columns: string[];
}

type InfoViewMode = 'table' | 'inputExample' | 'outputExample';

const countFormatter = new Intl.NumberFormat('ko-KR');
const actionTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

const panelLabels: Record<PanelKey, string> = {
  left: '테이블',
  center: '에디터',
  right: '실행 결과',
};

const panelMinWidths: Record<PanelKey, number> = {
  left: 300,
  center: 400,
  right: 320,
};

const editorThemes: Record<
  EditorThemeKey,
  { label: string; background: string; foreground: string; header: string; border: string; font: string }
> = {
  slate: {
    label: '슬레이트',
    background: '#0f172a',
    foreground: '#e2e8f0',
    header: '#162338',
    border: 'rgba(148, 163, 184, 0.24)',
    font: '"JetBrains Mono", "Fira Code", ui-monospace, SFMono-Regular, Consolas, monospace',
  },
  paper: {
    label: '페이퍼',
    background: '#f8fafc',
    foreground: '#0f172a',
    header: '#e2e8f0',
    border: 'rgba(148, 163, 184, 0.34)',
    font: '"IBM Plex Mono", ui-monospace, SFMono-Regular, Consolas, monospace',
  },
  forest: {
    label: '포레스트',
    background: '#0f1f1b',
    foreground: '#dcfce7',
    header: '#16332b',
    border: 'rgba(52, 211, 153, 0.22)',
    font: '"Cascadia Code", ui-monospace, SFMono-Regular, Consolas, monospace',
  },
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

function formatActionTime(date: Date) {
  return actionTimeFormatter.format(date);
}

function formatMs(value?: number) {
  if (value === undefined) {
    return '-';
  }

  return `${Math.round(value * 10) / 10}ms`;
}

function formatCount(value?: number) {
  if (value === undefined) {
    return '-';
  }

  return countFormatter.format(Math.round(value));
}

function parseSchemaEntries(schemaInfo: string): SchemaEntry[] {
  return schemaInfo
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const match = line.match(/^([^(]+)\((.+)\)$/);

      if (!match) {
        return { name: line, columns: [] };
      }

      return {
        name: match[1].trim(),
        columns: match[2]
          .split(',')
          .map((column) => column.trim())
          .filter(Boolean),
      };
    });
}

function getTopPercent(values: number[], currentValue: number) {
  if (values.length === 0) {
    return null;
  }

  const betterCount = values.filter((value) => value < currentValue).length;
  return Math.max(1, Math.round(((betterCount + 1) / (values.length + 1)) * 100));
}

function buildEditorThemeStyle(themeKey: EditorThemeKey): CSSProperties {
  const theme = editorThemes[themeKey];

  return {
    '--solve-editor-bg': theme.background,
    '--solve-editor-fg': theme.foreground,
    '--solve-editor-header': theme.header,
    '--solve-editor-border': theme.border,
    '--solve-editor-font': theme.font,
  } as EditorThemeStyle;
}

export default function ProblemSolvePage({ problemId }: ProblemSolvePageProps) {
  const problem = mockProblemDetailById[problemId] ?? mockProblemDetails[0];
  const availableDbms = getAvailableDbms(problem);
  const schemaEntries = parseSchemaEntries(problem.schemaInfo);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>(availableDbms[0] ?? problem.dbmsOptions[0] ?? 'postgresql');
  const [sql, setSql] = useState(problem.starterSql);
  const [result, setResult] = useState<MockResult | null>(null);
  const [lastActionLabel, setLastActionLabel] = useState<string | null>(null);
  const [lastActionAt, setLastActionAt] = useState<string | null>(null);
  const [infoViewMode, setInfoViewMode] = useState<InfoViewMode>('table');
  const [activeTableName, setActiveTableName] = useState<string>(() => schemaEntries[0]?.name ?? '');
  const [editorTheme, setEditorTheme] = useState<EditorThemeKey>('slate');
  const [panelVisibility, setPanelVisibility] = useState<PanelVisibilityState>({
    left: true,
    center: true,
    right: false,
  });
  const [editorDetached, setEditorDetached] = useState(false);
  const [panelWeights, setPanelWeights] = useState<Record<PanelKey, number>>({
    left: 25,
    center: 47,
    right: 28,
  });
  const [dragState, setDragState] = useState<DragState | null>(null);
  const workspaceRef = useRef<HTMLDivElement | null>(null);

  const runtimeDistribution = getRuntimeDistribution(problem, selectedDbms);
  const editorThemeStyle = buildEditorThemeStyle(editorTheme);
  const resolvedActiveTableName = schemaEntries.some((entry) => entry.name === activeTableName)
    ? activeTableName
    : schemaEntries[0]?.name ?? '';
  const resultTimePercent =
    result && runtimeDistribution ? getTopPercent(runtimeDistribution.samples.map((sample) => sample.timeMs), result.executionTimeMs) : null;
  const resultScanRowsPercent =
    result && runtimeDistribution ? getTopPercent(runtimeDistribution.samples.map((sample) => sample.rowsScanned), result.scanRows) : null;

  const mainPanelVisibility = {
    left: panelVisibility.left,
    center: panelVisibility.center && !editorDetached,
    right: panelVisibility.right,
  };

  const visibleMainPanels = (['left', 'center', 'right'] as PanelKey[]).filter((panelKey) => mainPanelVisibility[panelKey]);
  const visibleAnywhereCount =
    (panelVisibility.left ? 1 : 0) + (panelVisibility.center ? 1 : 0) + (panelVisibility.right ? 1 : 0);
  const hiddenPanelChips = (['left', 'center', 'right'] as PanelKey[]).filter((panelKey) => !panelVisibility[panelKey]);

  useEffect(() => {
    if (!dragState) {
      return;
    }

    const handleMove = (event: MouseEvent) => {
      if (!workspaceRef.current) {
        return;
      }

      const rect = workspaceRef.current.getBoundingClientRect();
      const deltaPercent = ((event.clientX - dragState.startX) / rect.width) * 100;
      const pairTotal = dragState.startLeftWeight + dragState.startRightWeight;
      const leftMinPercent = (panelMinWidths[dragState.leftKey] / rect.width) * 100;
      const rightMinPercent = (panelMinWidths[dragState.rightKey] / rect.width) * 100;

      let minLeft = leftMinPercent;
      let maxLeft = pairTotal - rightMinPercent;

      if (maxLeft < minLeft) {
        const midpoint = pairTotal / 2;
        minLeft = Math.min(minLeft, midpoint);
        maxLeft = Math.max(maxLeft, midpoint);
      }

      const nextLeftWeight = clamp(dragState.startLeftWeight + deltaPercent, minLeft, maxLeft);
      const nextRightWeight = pairTotal - nextLeftWeight;

      setPanelWeights((current) => ({
        ...current,
        [dragState.leftKey]: nextLeftWeight,
        [dragState.rightKey]: nextRightWeight,
      }));
    };

    const handleUp = () => {
      setDragState(null);
    };

    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);

    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [dragState]);

  const showResultPanel = () => {
    setPanelVisibility((current) => ({
      ...current,
      right: true,
    }));
  };

  const handleExecute = (actionLabel: '실행' | '제출') => {
    setLastActionLabel(actionLabel);
    setLastActionAt(formatActionTime(new Date()));
    setResult(problem.mockResult);
    showResultPanel();
  };

  const handleRun = () => {
    handleExecute('실행');
  };

  const handleSubmit = () => {
    handleExecute('제출');
  };

  const togglePanelVisibility = (panelKey: PanelKey) => {
    setPanelVisibility((current) => {
      if (current[panelKey] && visibleAnywhereCount <= 1) {
        return current;
      }

      if (panelKey === 'center' && current.center) {
        setEditorDetached(false);
      }

      return {
        ...current,
        [panelKey]: !current[panelKey],
      };
    });
  };

  const restorePanel = (panelKey: PanelKey) => {
    setPanelVisibility((current) => ({
      ...current,
      [panelKey]: true,
    }));

    if (panelKey === 'center') {
      setEditorDetached(false);
    }
  };

  const toggleEditorDetach = () => {
    if (!panelVisibility.center) {
      setPanelVisibility((current) => ({
        ...current,
        center: true,
      }));
    }

    setEditorDetached((current) => !current);
  };

  const renderTableContent = () => {
    const activeTable = schemaEntries.find((entry) => entry.name === resolvedActiveTableName) ?? schemaEntries[0];

    if (!activeTable) {
      return <div className="solve-result-empty">표시할 테이블 정보가 없습니다.</div>;
    }

    return (
      <div className="result-table-scroll solve-schema-table-wrap">
        <table className="result-table solve-schema-table">
          <thead>
            <tr>
              <th scope="col">순서</th>
              <th scope="col">컬럼명</th>
            </tr>
          </thead>
          <tbody>
            {activeTable.columns.length > 0 ? (
              activeTable.columns.map((column, index) => (
                <tr key={`${activeTable.name}-${column}`}>
                  <td>{index + 1}</td>
                  <td>{column}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={2}>등록된 컬럼 정보가 없습니다.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    );
  };

  const renderInfoContent = () => {
    if (infoViewMode === 'inputExample') {
      return <pre className="code-block solve-side-code">{problem.inputExample}</pre>;
    }

    if (infoViewMode === 'outputExample') {
      return <pre className="code-block solve-side-code">{problem.outputExample}</pre>;
    }

    return renderTableContent();
  };

  const renderPanelActions = (panelKey: PanelKey, extraAction?: ReactNode) => (
    <div className="solve-pane-actions">
      {extraAction}
      <button
        type="button"
        className="mini-toggle solve-pane-action solve-pane-action-icon"
        aria-label={`${panelLabels[panelKey]} 닫기`}
        onClick={() => togglePanelVisibility(panelKey)}
      >
        <span aria-hidden="true">×</span>
      </button>
    </div>
  );

  const leftPanel = (
    <section className="panel-card solve-pane solve-pane-left">
      <div className="solve-pane-header">
        <div>
          <p className="panel-meta">테이블 / 예시</p>
          <h2 className="panel-title">테이블/예시</h2>
        </div>
        {renderPanelActions('left')}
      </div>

      <div className="segmented solve-side-mode-switch" role="group" aria-label="테이블과 예시 보기">
        <button
          type="button"
          className={`segmented-btn ${infoViewMode === 'table' ? 'is-selected' : ''}`}
          aria-pressed={infoViewMode === 'table'}
          onClick={() => setInfoViewMode('table')}
        >
          테이블
        </button>
        <button
          type="button"
          className={`segmented-btn ${infoViewMode === 'inputExample' ? 'is-selected' : ''}`}
          aria-pressed={infoViewMode === 'inputExample'}
          onClick={() => setInfoViewMode('inputExample')}
        >
          입력 예시
        </button>
        <button
          type="button"
          className={`segmented-btn ${infoViewMode === 'outputExample' ? 'is-selected' : ''}`}
          aria-pressed={infoViewMode === 'outputExample'}
          onClick={() => setInfoViewMode('outputExample')}
        >
          출력 예시
        </button>
      </div>

      <div className={`solve-side-layout ${infoViewMode === 'table' ? '' : 'is-example'}`.trim()}>
        {infoViewMode === 'table' ? (
          <div className="solve-side-tabs" role="tablist" aria-label="테이블 탭">
            {schemaEntries.map((entry) => (
              <button
                key={entry.name}
                type="button"
                className={`solve-bookmark-button ${resolvedActiveTableName === entry.name ? 'is-selected' : ''}`}
                aria-pressed={resolvedActiveTableName === entry.name}
                onClick={() => setActiveTableName(entry.name)}
              >
                {entry.name}
              </button>
            ))}
          </div>
        ) : null}

        <div className="solve-side-panel">{renderInfoContent()}</div>
      </div>
    </section>
  );

  const editorPanel = (
    <section className="panel-card solve-pane solve-pane-editor" style={editorThemeStyle}>
      <div className="solve-pane-header">
        <div>
          <p className="panel-meta">SQL 작업 공간</p>
          <h2 className="panel-title">에디터</h2>
        </div>
        {renderPanelActions(
          'center',
          <button
            type="button"
            className={`mini-toggle solve-pane-action solve-pane-action-icon ${editorDetached ? 'is-selected' : ''}`}
            aria-label={editorDetached ? '에디터 다시 붙이기' : '에디터 분리'}
            onClick={toggleEditorDetach}
          >
            <span aria-hidden="true">{editorDetached ? '↙' : '↗'}</span>
          </button>,
        )}
      </div>

      <div className="solve-editor-toolbar">
        <div className="solve-theme-picker" role="group" aria-label="에디터 테마 선택">
          {Object.entries(editorThemes).map(([themeKey, theme]) => (
            <button
              key={themeKey}
              type="button"
              className={`mini-toggle solve-theme-button ${editorTheme === themeKey ? 'is-selected' : ''}`}
              onClick={() => setEditorTheme(themeKey as EditorThemeKey)}
            >
              {theme.label}
            </button>
          ))}
        </div>
      </div>

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
            <button type="button" className="btn secondary" onClick={handleRun} disabled={sql.trim().length === 0}>
              실행
            </button>
            <button type="button" className="btn primary" onClick={handleSubmit} disabled={sql.trim().length === 0}>
              제출
            </button>
          </div>
        </div>

        <textarea
          className="solve-sql-editor"
          spellCheck={false}
          value={sql}
          onChange={(event) => setSql(event.target.value)}
          aria-label="SQL 에디터"
        />
      </div>
    </section>
  );

  const rightPanel = (
    <section className="panel-card solve-pane solve-pane-right">
      <div className="solve-pane-header">
        <div>
          <p className="panel-meta">실행 결과</p>
          <h2 className="panel-title">실행 결과</h2>
        </div>
        {renderPanelActions('right')}
      </div>

      {result ? (
        <>
          <div className="solve-result-lines">
            <div className="solve-result-line">
              <span className="solve-result-label">판정</span>
              <strong className="solve-result-value">{result.status === 'success' ? '정답' : '재확인 필요'}</strong>
            </div>
            <div className="solve-result-line">
              <span className="solve-result-label">실행시간</span>
              <strong className="solve-result-value">{formatMs(result.executionTimeMs)}</strong>
            </div>
            <div className="solve-result-line">
              <span className="solve-result-label">스캔 행 수</span>
              <strong className="solve-result-value">{formatCount(result.scanRows)}</strong>
            </div>
          </div>

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
                <strong>{resultTimePercent ? `상위 ${resultTimePercent}%` : '비교 데이터 없음'}</strong>
              </div>
              <div className="solve-performance-item">
                <span>현재 스캔 행 수 구간</span>
                <strong>{resultScanRowsPercent ? `상위 ${resultScanRowsPercent}%` : '비교 데이터 없음'}</strong>
              </div>
            </div>
          </div>
        </>
      ) : (
        <div className="solve-result-empty">실행 또는 제출 후 결과가 이 영역에 표시됩니다.</div>
      )}
    </section>
  );

  const panelContent: Record<PanelKey, ReactNode> = {
    left: leftPanel,
    center: editorPanel,
    right: rightPanel,
  };

  const renderSplitter = (leftKey: PanelKey, rightKey: PanelKey) => (
    <button
      type="button"
      className="solve-pane-splitter"
      aria-label={`${panelLabels[leftKey]}와 ${panelLabels[rightKey]} 너비 조절`}
      onMouseDown={(event) => {
        event.preventDefault();
        setDragState({
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
      <div className="solve-page-topbar">
        <button type="button" className="btn secondary solve-back-button" onClick={() => navigate(PROBLEMS_PATH)}>
          문제 목록
        </button>
      </div>

      <section className="panel-card solve-page-hero">
        {lastActionAt && lastActionLabel ? (
          <div className="solve-page-hero-meta">
            <span className="subtle-chip">
              {lastActionLabel} {lastActionAt}
            </span>
          </div>
        ) : null}

        <div className="solve-page-hero-copy">
          <div className="solve-title-row">
            <span className="solve-problem-number">문제 {problem.number}</span>
            <h1 className="solve-problem-title">{problem.title}</h1>
          </div>
          <p className="content-text solve-problem-description">{problem.description}</p>
        </div>
      </section>

      <section className="panel-card solve-workspace-card">
        <div className="solve-workspace-toolbar">
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

          <div className="solve-workspace-toolbar-group">
            {hiddenPanelChips.length > 0 ? (
              hiddenPanelChips.map((panelKey) => (
                <button
                  key={panelKey}
                  type="button"
                  className="mini-toggle solve-restore-button"
                  aria-label={`${panelLabels[panelKey]} 다시 표시`}
                  onClick={() => restorePanel(panelKey)}
                >
                  <span className="solve-restore-icon" aria-hidden="true">
                    +
                  </span>
                  <span>{panelLabels[panelKey]}</span>
                </button>
              ))
            ) : (
              <span className="subtle-chip">모든 패널 표시 중</span>
            )}
          </div>
        </div>

        <div className="solve-workspace" ref={workspaceRef}>
          {visibleMainPanels.length === 0 ? (
            <div className="solve-workspace-empty">닫아둔 패널을 다시 열어주세요.</div>
          ) : (
            visibleMainPanels.map((panelKey, index) => (
              <div key={panelKey} className={`solve-workspace-segment solve-workspace-segment-${panelKey}`} style={{ flex: `${panelWeights[panelKey]} 1 0` }}>
                {panelContent[panelKey]}
                {index < visibleMainPanels.length - 1 ? renderSplitter(panelKey, visibleMainPanels[index + 1]) : null}
              </div>
            ))
          )}
        </div>
      </section>

      {panelVisibility.center && editorDetached ? (
        <section className="panel-card solve-floating-editor" style={editorThemeStyle}>
          <div className="solve-pane-header">
            <div>
              <p className="panel-meta">분리 에디터</p>
              <h2 className="panel-title">에디터 PIP</h2>
            </div>
            <div className="solve-pane-actions">
              <button
                type="button"
                className="mini-toggle solve-pane-action solve-pane-action-icon is-selected"
                aria-label="에디터 다시 붙이기"
                onClick={toggleEditorDetach}
              >
                <span aria-hidden="true">↙</span>
              </button>
              <button
                type="button"
                className="mini-toggle solve-pane-action solve-pane-action-icon"
                aria-label="에디터 닫기"
                onClick={() => togglePanelVisibility('center')}
              >
                <span aria-hidden="true">×</span>
              </button>
            </div>
          </div>
          <div className="solve-editor-toolbar">
            <div className="solve-theme-picker" role="group" aria-label="에디터 테마 선택">
              {Object.entries(editorThemes).map(([themeKey, theme]) => (
                <button
                  key={themeKey}
                  type="button"
                  className={`mini-toggle solve-theme-button ${editorTheme === themeKey ? 'is-selected' : ''}`}
                  onClick={() => setEditorTheme(themeKey as EditorThemeKey)}
                >
                  {theme.label}
                </button>
              ))}
            </div>
          </div>
          <div className="solve-editor-surface">
            <div className="solve-editor-surface-header">
              <span className="solve-editor-file">main.sql</span>
              <div className="solve-editor-actions">
                <button type="button" className="btn secondary" onClick={handleRun} disabled={sql.trim().length === 0}>
                  실행
                </button>
                <button type="button" className="btn primary" onClick={handleSubmit} disabled={sql.trim().length === 0}>
                  제출
                </button>
              </div>
            </div>
            <textarea
              className="solve-sql-editor"
              spellCheck={false}
              value={sql}
              onChange={(event) => setSql(event.target.value)}
              aria-label="SQL 에디터 PIP"
            />
          </div>
        </section>
      ) : null}
    </div>
  );
}
