import { mockDbmsOptions } from '../../mocks/dbms';
import type { DbmsType } from '../../types/domain';

interface DbmsSelectorProps {
  selectedDbms: DbmsType;
  onChange: (dbms: DbmsType) => void;
  disabledDbms: DbmsType[];
}

export default function DbmsSelector({ selectedDbms, onChange, disabledDbms }: DbmsSelectorProps) {
  return (
    <section className="panel-card compact">
      <div className="panel-heading-row responsive">
        <div>
          <p className="panel-meta">실행 환경</p>
          <h2 className="panel-title">DBMS 선택</h2>
        </div>
        <p className="hint-text">현재는 PostgreSQL만 선택할 수 있고 Oracle은 비활성 상태입니다.</p>
      </div>

      <div className="section-gate">
        {mockDbmsOptions.map((dbms) => {
          const isDisabled = dbms.disabled || disabledDbms.includes(dbms.id);
          return (
            <button
              key={dbms.id}
              type="button"
              disabled={isDisabled}
              onClick={() => onChange(dbms.id)}
              className={`mini-toggle ${selectedDbms === dbms.id ? 'is-selected' : ''}`}
            >
              {dbms.label}
              {isDisabled && <span className="tab-meta">준비중</span>}
            </button>
          );
        })}
      </div>
    </section>
  );
}
