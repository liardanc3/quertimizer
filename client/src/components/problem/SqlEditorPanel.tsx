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
      <div className="panel-heading-row">
        <h2 className="panel-title">SQL Editor</h2>
        <div className="editor-actions">
          <button type="button" className="btn ghost" onClick={() => setSql(initialSql)}>
            초기화
          </button>
          <button type="button" className="btn primary-soft" onClick={onRun}>
            실행
          </button>
          <button type="button" className="btn success-soft" onClick={onSubmit}>
            제출
          </button>
        </div>
      </div>
      <textarea className="sql-editor" value={sql} onChange={(event) => setSql(event.target.value)} spellCheck={false} />
      <p className="hint-text">TODO: API 연동 시 서버 실행 결과와 실제 제출 상태를 표시합니다.</p>
    </section>
  );
}
