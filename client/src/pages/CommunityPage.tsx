export default function CommunityPage() {
  return (
    <div className="page-stack">
      <section className="panel-card">
        <p className="panel-meta">Community</p>
        <h1 className="page-title">커뮤니티</h1>
        <p className="muted-text">문제 풀이 전략, 튜닝 팁, 공지와 토론을 모아두는 커뮤니티 공간입니다.</p>
      </section>

      <section className="panel-card">
        <div className="panel-heading-row responsive">
          <div>
            <p className="panel-meta">Coming Soon</p>
            <h2 className="panel-title">커뮤니티 허브 준비 중</h2>
          </div>
          <span className="subtle-chip">Preview</span>
        </div>
        <p className="content-text">
          추후에는 공지사항, 가이드라인, 풀이 공유, 질문 게시판을 이 영역에서 한 번에 탐색할 수 있도록 확장할
          예정입니다.
        </p>
      </section>
    </div>
  );
}
