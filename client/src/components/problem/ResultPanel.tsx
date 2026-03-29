import type { DbmsType, MockResult, ProblemDetail, RuntimeDistribution } from '../../types/domain';

interface ResultPanelProps {
  problem: ProblemDetail;
  result: MockResult;
  actionLabel: string;
  selectedDbms: DbmsType;
  lastActionAt: string | null;
}

const countFormatter = new Intl.NumberFormat('ko-KR');

function getDbmsLabel(dbms: DbmsType) {
  return dbms === 'postgresql' ? 'PostgreSQL' : 'Oracle';
}

function getRuntimeDistribution(problem: ProblemDetail, selectedDbms: DbmsType): RuntimeDistribution | undefined {
  return problem.runtimeDistributions?.[selectedDbms] ?? problem.runtimeDistribution;
}

function getOutputHeaders(outputExample: string) {
  const headerLine = outputExample.split('\n')[0] ?? '';

  return headerLine
    .split('|')
    .map((value) => value.trim())
    .filter(Boolean);
}

function getPerformanceSummary(result: MockResult, runtimeDistribution?: RuntimeDistribution) {
  if (!runtimeDistribution) {
    return '아직 비교할 제출 표본이 없습니다.';
  }

  const comparisonPoolSize = runtimeDistribution.samples.length + 1;
  const fasterRuns = runtimeDistribution.samples.filter((sample) => sample.timeMs < result.executionTimeMs).length;
  const rank = fasterRuns + 1;
  const topPercent = Math.max(1, Math.round((rank / comparisonPoolSize) * 100));

  if (result.executionTimeMs <= runtimeDistribution.fastestTimeMs) {
    return '현재 실행은 최고 기록과 비슷한 속도입니다.';
  }

  if (result.executionTimeMs <= runtimeDistribution.medianTimeMs) {
    return `현재 실행은 예상 상위 ${topPercent}% 구간입니다.`;
  }

  return `현재 실행은 중앙값보다 느린 편이며 예상 상위 ${topPercent}% 구간입니다.`;
}

export default function ResultPanel({
  problem,
  result,
  actionLabel,
  selectedDbms,
  lastActionAt,
}: ResultPanelProps) {
  const runtimeDistribution = getRuntimeDistribution(problem, selectedDbms);
  const outputHeaders = getOutputHeaders(problem.outputExample);
  const judgeChecks = [
    {
      label: '채점 상태',
      value: result.status === 'success' ? '정답 통과' : '재점검 필요',
      tone: result.status === 'success' ? 'is-success' : 'is-warning',
    },
    {
      label: '인덱스 사용',
      value: result.indexUsed ? '활용됨' : '미활용',
      tone: result.indexUsed ? 'is-success' : 'is-warning',
    },
    {
      label: 'Full Scan',
      value: result.fullScan ? '발생' : '없음',
      tone: result.fullScan ? 'is-warning' : 'is-success',
    },
    {
      label: '실행 환경',
      value: getDbmsLabel(selectedDbms),
      tone: 'is-neutral',
    },
  ];

  return (
    <section className="panel-card result-panel">
      <div className="panel-heading-row responsive">
        <div>
          <p className="panel-meta">Judge Result</p>
          <h2 className="panel-title">채점 결과</h2>
        </div>
        <span className={`status-pill ${result.status === 'success' ? 'ok' : 'fail'}`}>
          {result.status === 'success' ? 'SUCCESS' : 'FAIL'}
        </span>
      </div>

      <p className="content-text">
        [{actionLabel}] {result.message}
      </p>
      <p className="hint-text">
        {getDbmsLabel(selectedDbms)} 기준으로 평가 중{lastActionAt ? ` · ${lastActionAt}` : ''}
      </p>

      <div className="judge-check-grid">
        {judgeChecks.map((item) => (
          <div key={item.label} className={`judge-check-item ${item.tone}`.trim()}>
            <span className="judge-check-label">{item.label}</span>
            <strong className="judge-check-value">{item.value}</strong>
          </div>
        ))}
      </div>

      <dl className="metrics-grid">
        <div className="metric-card">
          <dt>실행 시간</dt>
          <dd>{result.executionTimeMs.toFixed(1)} ms</dd>
        </div>
        <div className="metric-card">
          <dt>추정 Cost</dt>
          <dd>{countFormatter.format(result.cost)}</dd>
        </div>
        <div className="metric-card">
          <dt>최고 기록</dt>
          <dd>{runtimeDistribution ? `${runtimeDistribution.fastestTimeMs} ms` : '-'}</dd>
        </div>
        <div className="metric-card">
          <dt>중앙값</dt>
          <dd>{runtimeDistribution ? `${runtimeDistribution.medianTimeMs} ms` : '-'}</dd>
        </div>
      </dl>

      <p className="inline-note">{getPerformanceSummary(result, runtimeDistribution)}</p>

      <div className="table-like result-table-wrap">
        <div className="panel-heading-row responsive">
          <p className="panel-meta">결과 미리보기</p>
          <span className="subtle-chip">{result.rows.length} rows</span>
        </div>
        <div className="result-table-scroll">
          <table className="result-table">
            {outputHeaders.length > 0 ? (
              <thead>
                <tr>
                  {outputHeaders.map((header) => (
                    <th key={header} scope="col">
                      {header}
                    </th>
                  ))}
                </tr>
              </thead>
            ) : null}
            <tbody>
              {result.rows.map((row, index) => (
                <tr key={`${row.columns.join('-')}-${index}`}>
                  {row.columns.map((value, columnIndex) => (
                    <td key={`${index}-${columnIndex}`}>{value}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
