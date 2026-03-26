import SectionBadge from '../common/SectionBadge';

export default function RankingDisabledCard() {
  return (
    <section className="panel-card">
      <div className="panel-heading-row">
        <h2 className="panel-title">랭킹</h2>
        <SectionBadge label="곧 공개" disabled />
      </div>
      <p className="muted-text">성능 점수 기반 시즌 랭킹, DBMS 별 래더, 구간별 통계가 제공될 예정입니다.</p>
    </section>
  );
}
