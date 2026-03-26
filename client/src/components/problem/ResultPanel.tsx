import type { MockResult } from '../../types/domain';

interface ResultPanelProps {
  result: MockResult;
  actionLabel: string;
}

export default function ResultPanel({ result, actionLabel }: ResultPanelProps) {
  return (
    <section className="panel-card">
      <div className="panel-heading-row">
        <h2 className="panel-title">실행/채점 결과</h2>
        <span className={`status-pill ${result.status === 'success' ? 'ok' : 'fail'}`}>
          {result.status === 'success' ? 'SUCCESS' : 'FAIL'}
        </span>
      </div>
      <p className="content-text">[{actionLabel}] {result.message}</p>

      <dl className="metrics-grid">
        <div className="metric-card"><dt>실행 시간</dt><dd>{result.executionTimeMs} ms</dd></div>
        <div className="metric-card"><dt>비용(Cost)</dt><dd>{result.cost}</dd></div>
        <div className="metric-card"><dt>인덱스 사용</dt><dd>{result.indexUsed ? '사용' : '미사용'}</dd></div>
        <div className="metric-card"><dt>풀 스캔</dt><dd>{result.fullScan ? '발생' : '없음'}</dd></div>
      </dl>

      <div className="table-like">
        <p className="panel-meta">결과 미리보기</p>
        {result.rows.map((row, index) => (
          <p key={`${row.columns.join('-')}-${index}`} className="row-line">{row.columns.join(' | ')}</p>
        ))}
      </div>
    </section>
  );
}
