import type { Dispatch, SetStateAction } from 'react';

interface SqlEditorPanelProps {
  sql: string;
  setSql: Dispatch<SetStateAction<string>>;
  initialSql: string;
  onRun: () => void;
  onSubmit: () => void;
}

export default function SqlEditorPanel({ sql, setSql, initialSql, onRun, onSubmit }: SqlEditorPanelProps) {
  return (
    <section className="panel-card">
      <div className="panel-heading-row responsive">
        <div>
          <p className="panel-meta">SQL Workspace</p>
          <h2 className="panel-title">쿼리 편집기</h2>
        </div>
        <div className="editor-actions">
          <button type="button" className="btn ghost" onClick={() => setSql(initialSql)}>
            초기화
          </button>
          <button type="button" className="btn secondary" onClick={onRun}>
            실행
          </button>
          <button type="button" className="btn primary" onClick={onSubmit}>
            제출
          </button>
        </div>
      </div>

      <div className="editor-surface">
        <div className="editor-surface-header">
          <span className="editor-file-name">main.sql</span>
          <span className="subtle-chip inverted">Mock Runner</span>
        </div>
        <textarea className="sql-editor" value={sql} onChange={(event) => setSql(event.target.value)} spellCheck={false} />
      </div>

      <p className="hint-text">TODO: API 연동 시 실제 실행 결과, 제출 상태, 히스토리 로그를 연결합니다.</p>
    </section>
  );
}
