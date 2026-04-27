import { mockDbmsOptions } from '../../mocks/dbms';
import type { DbmsType } from '../../types/domain';

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
  disabledDbms,
}: DbmsSelectorProps) {
  const visibleDbms = mockDbmsOptions.filter((dbms) => supportedDbms.includes(dbms.id));
  const availableCount = visibleDbms.filter((dbms) => !dbms.disabled && !disabledDbms.includes(dbms.id)).length;
  const disabledCount = visibleDbms.length - availableCount;

  return (
    <section className="panel-card compact solve-dbms-card">
      <div className="panel-heading-row responsive">
        <div>
          <p className="panel-meta">실행 환경</p>
          <h2 className="panel-title">DBMS 선택</h2>
        </div>
        <div className="solve-dbms-summary">
          <span className="subtle-chip">{availableCount}개 사용 가능</span>
          <span className="subtle-chip">{disabledCount}개 준비 중</span>
        </div>
      </div>

      <div className="section-gate">
        {visibleDbms.map((dbms) => {
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
              {isDisabled ? <span className="tab-meta">준비 중</span> : null}
            </button>
          );
        })}
      </div>

      <p className="hint-text">
        선택한 DBMS 기준으로 문제 설명과 실행 통계를 표시합니다.
      </p>
    </section>
  );
}
