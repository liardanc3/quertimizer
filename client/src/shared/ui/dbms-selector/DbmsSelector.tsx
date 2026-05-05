import type { DbmsType } from '@/shared/api/domain';

const DBMS_OPTIONS: Array<{ id: DbmsType; label: string }> = [
  { id: 'postgresql', label: 'PostgreSQL' },
  { id: 'mysql', label: 'MySQL' },
];

interface DbmsSelectorProps {
  selectedDbms: DbmsType;
  onChange: (dbms: DbmsType) => void;
  supportedDbms: DbmsType[];
  disabledDbms: DbmsType[];
}

export default function DbmsSelector({
  selectedDbms,
  onChange,
  supportedDbms,
}: DbmsSelectorProps) {
  const visibleDbms = DBMS_OPTIONS.filter((dbms) => supportedDbms.includes(dbms.id));
  const availableCount = visibleDbms.length;

  return (
    <section className="panel-card compact solve-dbms-card">
      <div className="panel-heading-row responsive">
        <div>
          <p className="panel-meta">실행 환경</p>
          <h2 className="panel-title">DBMS 선택</h2>
        </div>
        <div className="solve-dbms-summary">
          <span className="subtle-chip">{availableCount}개 사용 가능</span>
        </div>
      </div>

      <div className="section-gate">
        {visibleDbms.map((dbms) => (
          <button
            key={dbms.id}
            type="button"
            onClick={() => onChange(dbms.id)}
            className={`mini-toggle ${selectedDbms === dbms.id ? 'is-selected' : ''}`}
          >
            {dbms.label}
          </button>
        ))}
      </div>

      <p className="hint-text">
        선택한 DBMS 기준으로 문제 설명과 실행 통계를 표시합니다.
      </p>
    </section>
  );
}
