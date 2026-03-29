import { mockProfile } from '../mocks/profile';

export default function ProfilePage() {
  return (
    <div className="page-stack">
      <section className="panel-card">
        <p className="panel-meta">Profile</p>
        <h1 className="page-title">{mockProfile.name}</h1>
        <p className="muted-text">개인 프로필과 학습 진행 상황을 확인할 수 있는 공간입니다.</p>
      </section>

      <section className="panel-card compact">
        <dl className="metrics-grid">
          <div className="metric-card">
            <dt>티어</dt>
            <dd>{mockProfile.tier}</dd>
          </div>
          <div className="metric-card">
            <dt>해결 문제</dt>
            <dd>{mockProfile.solvedCount}문제</dd>
          </div>
        </dl>
      </section>
    </div>
  );
}
