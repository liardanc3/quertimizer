import SectionBadge from '../common/SectionBadge';
import { useUiText } from '../../lib/uiText';

export default function RankingDisabledCard() {
  const { text } = useUiText();
  const rankingFeatures = [
    text('RANKING_DISABLED_FEATURE_1', '성능 점수 기반 시즌 래더'),
    text('RANKING_DISABLED_FEATURE_2', 'DBMS별 상위 사용자 비교'),
    text('RANKING_DISABLED_FEATURE_3', '티어 구간별 통계 리포트'),
  ];

  return (
    <section className="panel-card disabled-panel">
      <div className="panel-heading-row responsive">
        <div>
          <p className="panel-meta">{text('RANKING_DISABLED_META_LABEL', '준비 중')}</p>
          <h2 className="panel-title">{text('HEADER_MENU_RANKING', '랭킹')}</h2>
        </div>
        <SectionBadge label={text('RANKING_DISABLED_BADGE_LABEL', '곧 공개')} disabled />
      </div>

      <p className="muted-text">{text('RANKING_DISABLED_DESC', '정답률뿐 아니라 실행 성능까지 반영한 시즌 랭킹과 래더 화면이 추가될 예정입니다.')}</p>

      <div className="disabled-feature-list">
        {rankingFeatures.map((feature) => (
          <div key={feature} className="disabled-feature-item">
            {feature}
          </div>
        ))}
      </div>
    </section>
  );
}
