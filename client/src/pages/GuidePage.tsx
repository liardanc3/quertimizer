import './AdminPage.css';

export default function GuidePage() {
  return (
    <div className="page-stack admin-page">
      <div className="admin-page-header">
        <div className="admin-page-tab-row" role="tablist" aria-label="가이드">
          <button type="button" role="tab" aria-selected={true} className="admin-page-tab is-selected">
            문제 생성
          </button>
        </div>
      </div>

      <section className="panel-card">
        <div className="page-stack">
          <p className="content-text">
            문제 생성을 원하시면{' '}
            <a href="mailto:quertimizer@gmail.com">quertimizer@gmail.com</a>
            으로 문의 메일을 남겨 주세요.
          </p>
        </div>
      </section>
    </div>
  );
}
