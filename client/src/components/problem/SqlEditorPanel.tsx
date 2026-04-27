import type { Dispatch, SetStateAction } from 'react';
import { useUiText } from '../../lib/uiText';
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
  return dbms === 'postgresql' ? 'PostgreSQL' : 'MySQL';
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
  const { text } = useUiText();
  const isDirty = sql !== initialSql;
  const isEmpty = sql.trim().length === 0;
  const lineCount = sql.split('\n').length;
  const characterCount = sql.length;
  const statusCards = [
    {
      label: text('SQL_EDITOR_STATUS_LABEL', '작업 상태'),
      value: isDirty ? text('SQL_EDITOR_DIRTY_VALUE', '수정됨') : text('SQL_EDITOR_INITIAL_VALUE', '초기 코드'),
    },
    {
      label: text('SQL_EDITOR_QUERY_LENGTH_LABEL', '쿼리 길이'),
      value: text('SQL_EDITOR_QUERY_LENGTH_VALUE', { lineCount, characterCount }, `${lineCount}줄 / ${characterCount}자`),
    },
    {
      label: text('SQL_EDITOR_EXECUTION_ENV_LABEL', '실행 환경'),
      value: getDbmsLabel(selectedDbms),
    },
    {
      label: text('SQL_EDITOR_LAST_ACTION_LABEL', '최근 액션'),
      value: lastActionAt ? `${actionLabel} · ${lastActionAt}` : text('SQL_EDITOR_NO_ACTION_VALUE', '아직 실행 전'),
    },
  ];

  return (
    <section className="panel-card">
      <div className="panel-heading-row responsive">
        <div>
          <p className="panel-meta">{text('SQL_EDITOR_META_LABEL', 'SQL 작업 공간')}</p>
          <h2 className="panel-title">{text('SQL_EDITOR_TITLE', '제출 에디터')}</h2>
        </div>
        <div className="editor-actions">
          <button type="button" className="btn ghost" onClick={() => setSql(initialSql)} disabled={!isDirty}>
            {text('SQL_EDITOR_RESET_BUTTON', '초기화')}
          </button>
          <button type="button" className="btn secondary" onClick={onRun} disabled={isEmpty}>
            {text('SQL_EDITOR_RUN_BUTTON', '실행')}
          </button>
          <button type="button" className="btn primary" onClick={onSubmit} disabled={isEmpty}>
            {text('SQL_EDITOR_SUBMIT_BUTTON', '제출')}
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
          <span className="subtle-chip inverted">{text('SQL_EDITOR_MOCK_JUDGE_LABEL', '모의 채점')}</span>
        </div>
        <textarea
          className="sql-editor"
          value={sql}
          onChange={(event) => setSql(event.target.value)}
          spellCheck={false}
          aria-label={text('SQL_EDITOR_ARIA_LABEL', 'SQL 에디터')}
        />
      </div>

      <div className="editor-helper-grid">
        <p className="inline-note">
          {text('SQL_EDITOR_HELP_PRIMARY', '실행은 결과 미리보기와 성능 지표를 빠르게 확인하는 용도이고, 제출은 정답 여부와 성능 조건을 함께 보는 흐름으로 설계했습니다.')}
        </p>
        <p className="inline-note">
          {text('SQL_EDITOR_HELP_SECONDARY', '현재는 목데이터 기반 화면이므로 이후 API를 연결할 때 제출 대기, 히스토리, 실패 로그, 자동 저장 상태를 바로 확장할 수 있습니다.')}
        </p>
      </div>
    </section>
  );
}
