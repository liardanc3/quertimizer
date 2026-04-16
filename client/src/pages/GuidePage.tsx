import './GuidePage.css';

export default function GuidePage() {
  return (
    <div className="page-stack guide-page">
      <div className="guide-page-header">
        <div className="guide-page-tab-row" role="tablist" aria-label="가이드">
          <button type="button" role="tab" aria-selected={true} className="guide-page-tab is-selected">
            문제 생성
          </button>
        </div>
      </div>

      <section className="panel-card compact guide-page-card">
        <div className="guide-page-copy">
          <p className="guide-page-text">
            문제 생성을 원하시면{' '}
            <a href="mailto:quertimizer@gmail.com">quertimizer@gmail.com</a>
            으로 문의 메일을 남겨 주세요.
          </p>
        </div>
      </section>
    </div>
  );
}
