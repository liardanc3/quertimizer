import { useState } from 'react';
import DomainTabs from '../components/home/DomainTabs';
import HomeSectionGate from '../components/home/HomeSectionGate';
import ProblemList from '../components/home/ProblemList';
import ProblemModeSwitch from '../components/home/ProblemModeSwitch';
import RankingDisabledCard from '../components/home/RankingDisabledCard';
import type { DomainType, HomeSectionType, ProblemViewMode } from '../types/domain';

export default function HomePage() {
  const [domain, setDomain] = useState<DomainType>('rdbms');
  const [homeSection, setHomeSection] = useState<HomeSectionType>('problems');
  const [problemMode, setProblemMode] = useState<ProblemViewMode>('tagged');

  return (
    <div className="page-stack">
      <section>
        <h1 className="page-title">문제 풀이 대시보드</h1>
        <p className="muted-text">정답뿐 아니라 실행 성능까지 평가하는 SQL 튜닝 훈련을 시작하세요.</p>
      </section>

      <DomainTabs selectedDomain={domain} onChange={setDomain} />

      <div id="panel-rdbms" role="tabpanel" aria-labelledby="tab-rdbms" hidden={domain !== 'rdbms'}>
        <HomeSectionGate selectedSection={homeSection} onChange={setHomeSection} />

        <div className="home-grid">
          <RankingDisabledCard />

          <section className="panel-card">
            <div className="panel-heading-row responsive">
              <div>
                <h2 className="panel-title">문제 목록</h2>
                <p className="hint-text">현재는 RDBMS(PostgreSQL) 트랙만 제공됩니다.</p>
              </div>
              <ProblemModeSwitch mode={problemMode} onChange={setProblemMode} />
            </div>
            <ProblemList mode={problemMode} />
          </section>
        </div>
      </div>

      <div id="panel-nosql" role="tabpanel" aria-labelledby="tab-nosql" hidden={domain !== 'nosql'}>
        <section className="panel-card">
          <h2 className="panel-title">NoSQL 트랙 준비중</h2>
          <p className="content-text">문서형/키밸류 DB 대상 성능 문제 세트는 추후 오픈됩니다.</p>
        </section>
      </div>
    </div>
  );
}
