import type { Dispatch, SetStateAction } from 'react';
import type { DbmsType } from '../../types/domain';

interface SqlEditorPanelProps {
  sql: string;
  setSql: Dispatch<SetStateAction<string>>;
  initialSql: string;
  selectedDbms: DbmsType;
  actionLabel: string;
  lastActionAt: string | null;
  onRun: () => void;
  onSubmit: () => void;
}

function getDbmsLabel(dbms: DbmsType) {
  return dbms === 'postgresql' ? 'PostgreSQL' : 'Oracle';
}

export default function SqlEditorPanel({
  sql,
  setSql,
  initialSql,
  selectedDbms,
  actionLabel,
  lastActionAt,
  onRun,
  onSubmit,
}: SqlEditorPanelProps) {
  const isDirty = sql !== initialSql;
  const isEmpty = sql.trim().length === 0;
  const lineCount = sql.split('\n').length;
  const characterCount = sql.length;
  const statusCards = [
    {
      label: '작업 상태',
      value: isDirty ? '수정됨' : '초기 코드',
    },
    {
      label: '쿼리 길이',
      value: `${lineCount} lines / ${characterCount} chars`,
    },
    {
      label: '실행 환경',
      value: getDbmsLabel(selectedDbms),
    },
    {
      label: '최근 액션',
      value: lastActionAt ? `${actionLabel} · ${lastActionAt}` : '아직 실행 전',
    },
  ];

  return (
    <section className="panel-card">
      <div className="panel-heading-row responsive">
        <div>
          <p className="panel-meta">SQL Workspace</p>
          <h2 className="panel-title">제출 에디터</h2>
        </div>
        <div className="editor-actions">
          <button type="button" className="btn ghost" onClick={() => setSql(initialSql)} disabled={!isDirty}>
            초기화
          </button>
          <button type="button" className="btn secondary" onClick={onRun} disabled={isEmpty}>
            실행
          </button>
          <button type="button" className="btn primary" onClick={onSubmit} disabled={isEmpty}>
            제출
          </button>
        </div>
      </div>

      <div className="editor-status-strip">
        {statusCards.map((item) => (
          <div key={item.label} className="editor-stat-card">
            <span className="editor-stat-label">{item.label}</span>
            <strong className="editor-stat-value">{item.value}</strong>
          </div>
        ))}
      </div>

      <div className="editor-surface">
        <div className="editor-surface-header">
          <div className="editor-surface-meta">
            <span className="editor-file-name">main.sql</span>
            <span className="subtle-chip inverted">{getDbmsLabel(selectedDbms)}</span>
          </div>
          <span className="subtle-chip inverted">Mock Judge</span>
        </div>
        <textarea
          className="sql-editor"
          value={sql}
          onChange={(event) => setSql(event.target.value)}
          spellCheck={false}
          aria-label="SQL editor"
        />
      </div>

      <div className="editor-helper-grid">
        <p className="inline-note">
          실행은 결과 미리보기와 성능 지표를 빠르게 확인하는 용도이고, 제출은 정답 여부와 성능 조건을 함께 보는 흐름으로
          설계했습니다.
        </p>
        <p className="inline-note">
          현재는 목데이터 기반 화면이므로 이후 API를 연결할 때 제출 대기, 히스토리, 실패 로그, 자동 저장 상태를 바로
          확장할 수 있습니다.
        </p>
      </div>
    </section>
  );
}
