import { mockDbmsOptions } from '../../mocks/dbms';
import type { DbmsType } from '../../types/domain';

interface DbmsSelectorProps {
  selectedDbms: DbmsType;
  onChange: (dbms: DbmsType) => void;
  disabledDbms: DbmsType[];
}

export default function DbmsSelector({ selectedDbms, onChange, disabledDbms }: DbmsSelectorProps) {
  return (
    <div className="panel-card compact">
      <p className="panel-meta">실행 환경 (DBMS)</p>
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
    </div>
  );
}
