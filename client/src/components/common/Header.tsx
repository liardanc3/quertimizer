import { mockProfile } from '../../mocks/profile';
import { navigate } from '../../lib/navigation';

export default function Header() {
  return (
    <header className="header">
      <div className="header-inner">
        <button type="button" className="logo-button" onClick={() => navigate('/')}>
          <p className="logo-title">speedql</p>
          <p className="logo-subtitle">SQL 튜닝 트레이닝 플랫폼</p>
        </button>
        <div className="header-actions">
          <button type="button" className="btn ghost">
            로그인
          </button>
          <button type="button" className="btn primary-soft">
            회원가입
          </button>
          <button type="button" className="profile-chip">
            <span>{mockProfile.name}</span>
            <span>
              {mockProfile.tier} · {mockProfile.solvedCount}문제 해결
            </span>
          </button>
        </div>
      </div>
    </header>
  );
}
