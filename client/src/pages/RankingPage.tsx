import { mockProfile } from '../mocks/profile';
import { mockRanking } from '../mocks/ranking';
import { useMockSession } from '../lib/session';

const seasonStats = [
  { label: '현재 시즌', value: 'Season 01' },
  { label: '평가 기준', value: '정답률 + 성능 점수' },
  { label: '오픈 DBMS', value: 'PostgreSQL' },
];

export default function RankingPage() {
  const { isAuthenticated } = useMockSession();
  const myEntry = isAuthenticated ? mockRanking.find((entry) => entry.name === mockProfile.name) : undefined;

  return (
    <div className="page-stack">
      <section className="panel-card">
        <p className="panel-meta">Ranking</p>
        <h1 className="page-title">랭킹</h1>
        <p className="muted-text">정답 통과율과 실행 성능을 함께 반영한 목업 시즌 리더보드입니다.</p>
      </section>

      <section className="panel-card">
        <div className="stat-grid compact">
          {seasonStats.map((stat) => (
            <article key={stat.label} className="stat-card">
              <p className="stat-label">{stat.label}</p>
              <p className="stat-value">{stat.value}</p>
            </article>
          ))}
          {myEntry ? (
            <article className="stat-card">
              <p className="stat-label">내 순위</p>
              <p className="stat-value">{myEntry.rank}위</p>
            </article>
          ) : null}
        </div>
      </section>

      <section className="panel-card">
        <div className="panel-heading-row responsive">
          <div>
            <p className="panel-meta">Leaderboard</p>
            <h2 className="panel-title">시즌 리더보드</h2>
          </div>
          <span className="subtle-chip">Mock Data</span>
        </div>

        <div className="ranking-table">
          <div className="ranking-row ranking-head">
            <span>순위</span>
            <span>사용자</span>
            <span>티어</span>
            <span>점수</span>
            <span>해결 문제</span>
            <span>평균 실행 시간</span>
          </div>

          {mockRanking.map((entry) => (
            <div
              key={entry.rank}
              className={`ranking-row ${isAuthenticated && entry.name === mockProfile.name ? 'is-highlight' : ''}`}
            >
              <span>{entry.rank}</span>
              <span>{entry.name}</span>
              <span>{entry.tier}</span>
              <span>{entry.score.toLocaleString()}</span>
              <span>{entry.solvedCount}문제</span>
              <span>{entry.avgExecutionTimeMs.toFixed(1)} ms</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
