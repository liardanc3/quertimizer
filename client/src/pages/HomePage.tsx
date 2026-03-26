import { useState } from 'react';
import DomainTabs from '../components/home/DomainTabs';
import ProblemList from '../components/home/ProblemList';
import ProblemModeSwitch from '../components/home/ProblemModeSwitch';
import { mockProblems } from '../mocks/problems';
import type { DomainType, ProblemViewMode } from '../types/domain';

export default function HomePage() {
  const [domain, setDomain] = useState<DomainType>('rdbms');
  const [problemMode, setProblemMode] = useState<ProblemViewMode>('tagged');

  return (
    <div className="page-stack">
      <section className="panel-card">
        <p className="panel-meta">Problems</p>
        <h1 className="page-title">문제</h1>
        <p className="muted-text">현재 공개된 RDBMS 문제를 확인하고 바로 SQL 풀이 화면으로 이동할 수 있습니다.</p>
      </section>

      <DomainTabs selectedDomain={domain} onChange={setDomain} />

      <div id="panel-rdbms" role="tabpanel" aria-labelledby="tab-rdbms" hidden={domain !== 'rdbms'}>
        <section className="panel-card">
          <div className="panel-heading-row responsive">
            <div>
              <p className="panel-meta">문제 탐색</p>
              <h2 className="panel-title">RDBMS 문제 목록</h2>
              <p className="hint-text">
                현재는 PostgreSQL 트랙만 열려 있으며, 총 {mockProblems.length}개의 목업 문제가 준비되어
                있습니다.
              </p>
            </div>
            <ProblemModeSwitch mode={problemMode} onChange={setProblemMode} />
          </div>
          <ProblemList mode={problemMode} />
        </section>
      </div>

      <div id="panel-nosql" role="tabpanel" aria-labelledby="tab-nosql" hidden={domain !== 'nosql'}>
        <section className="panel-card disabled-panel">
          <div className="panel-heading-row responsive">
            <div>
              <p className="panel-meta">준비 중인 영역</p>
              <h2 className="panel-title">NoSQL 트랙</h2>
            </div>
            <span className="section-badge is-disabled">Coming Soon</span>
          </div>
          <p className="content-text">
            문서형 데이터 모델, 분산 저장 구조, NoSQL 전용 성능 문제 세트는 후속 단계에서 공개할 예정입니다.
          </p>
        </section>
      </div>
    </div>
  );
}
