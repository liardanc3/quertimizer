import { useUiText, type UiTextParams } from '../../lib/uiText';
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
  return dbms === 'postgresql' ? 'PostgreSQL' : 'MySQL';
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

function getPerformanceSummary(
  result: MockResult,
  runtimeDistribution: RuntimeDistribution | undefined,
  text: (key: string, paramsOrFallback?: string | UiTextParams, fallbackValue?: string) => string,
) {
  if (!runtimeDistribution) {
    return text('RESULT_PANEL_EMPTY_COMPARISON_MESSAGE', '아직 비교할 제출 표본이 없습니다.');
  }

  const comparisonPoolSize = runtimeDistribution.samples.length + 1;
  const fasterRuns = runtimeDistribution.samples.filter((sample) => sample.timeMs < result.executionTimeMs).length;
  const rank = fasterRuns + 1;
  const topPercent = Math.max(1, Math.round((rank / comparisonPoolSize) * 100));

  if (result.executionTimeMs <= runtimeDistribution.fastestTimeMs) {
    return text('RESULT_PANEL_FASTEST_MESSAGE', '현재 실행은 최고 기록과 비슷한 속도입니다.');
  }

  if (result.executionTimeMs <= runtimeDistribution.medianTimeMs) {
    return text('RESULT_PANEL_TOP_PERCENT_MESSAGE', { topPercent }, `현재 실행은 예상 상위 ${topPercent}% 구간입니다.`);
  }

  return text('RESULT_PANEL_SLOWER_MESSAGE', { topPercent }, `현재 실행은 중앙값보다 느린 편이며 예상 상위 ${topPercent}% 구간입니다.`);
}

export default function ResultPanel({
  problem,
  result,
  actionLabel,
  selectedDbms,
  lastActionAt,
}: ResultPanelProps) {
  const { text } = useUiText();
  const runtimeDistribution = getRuntimeDistribution(problem, selectedDbms);
  const outputHeaders = getOutputHeaders(problem.outputExample);
  const judgeChecks = [
    {
      label: text('RESULT_PANEL_JUDGE_STATUS_LABEL', '채점 상태'),
      value: result.status === 'success' ? text('RESULT_PANEL_JUDGE_STATUS_SUCCESS_VALUE', '정답 통과') : text('RESULT_PANEL_JUDGE_STATUS_REVIEW_VALUE', '재점검 필요'),
      tone: result.status === 'success' ? 'is-success' : 'is-warning',
    },
    {
      label: text('RESULT_PANEL_INDEX_USAGE_LABEL', '인덱스 사용'),
      value: result.indexUsed ? text('RESULT_PANEL_INDEX_USED_VALUE', '활용됨') : text('RESULT_PANEL_INDEX_UNUSED_VALUE', '미활용'),
      tone: result.indexUsed ? 'is-success' : 'is-warning',
    },
    {
      label: text('RESULT_PANEL_FULL_SCAN_LABEL', '전체 스캔'),
      value: result.fullScan ? text('RESULT_PANEL_FULL_SCAN_YES_VALUE', '발생') : text('COMMON_NONE_LABEL', '없음'),
      tone: result.fullScan ? 'is-warning' : 'is-success',
    },
    {
      label: text('RESULT_PANEL_EXECUTION_ENV_LABEL', '실행 환경'),
      value: getDbmsLabel(selectedDbms),
      tone: 'is-neutral',
    },
  ];

  return (
    <section className="panel-card result-panel">
      <div className="panel-heading-row responsive">
        <div>
          <p className="panel-meta">{text('RESULT_PANEL_META_LABEL', '채점 결과')}</p>
          <h2 className="panel-title">{text('RESULT_PANEL_TITLE', '채점 결과')}</h2>
        </div>
        <span className={`status-pill ${result.status === 'success' ? 'ok' : 'fail'}`}>
          {result.status === 'success' ? text('RESULT_PANEL_SUCCESS_LABEL', '정답') : text('RESULT_PANEL_FAIL_LABEL', '실패')}
        </span>
      </div>

      <p className="content-text">
        [{actionLabel}] {result.message}
      </p>
      <p className="hint-text">
        {text('RESULT_PANEL_EXECUTING_HINT_LABEL', { dbms: getDbmsLabel(selectedDbms) }, `${getDbmsLabel(selectedDbms)} 기준으로 평가 중`)}
        {lastActionAt ? ` · ${lastActionAt}` : ''}
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
          <dt>{text('RESULT_PANEL_EXECUTION_TIME_LABEL', '실행 시간')}</dt>
          <dd>{result.executionTimeMs.toFixed(1)} ms</dd>
        </div>
        <div className="metric-card">
          <dt>{text('RESULT_PANEL_ESTIMATED_COST_LABEL', '추정 Cost')}</dt>
          <dd>{countFormatter.format(result.cost)}</dd>
        </div>
        <div className="metric-card">
          <dt>{text('RESULT_PANEL_BEST_RECORD_LABEL', '최고 기록')}</dt>
          <dd>{runtimeDistribution ? `${runtimeDistribution.fastestTimeMs} ms` : '-'}</dd>
        </div>
        <div className="metric-card">
          <dt>{text('RESULT_PANEL_MEDIAN_LABEL', '중앙값')}</dt>
          <dd>{runtimeDistribution ? `${runtimeDistribution.medianTimeMs} ms` : '-'}</dd>
        </div>
      </dl>

      <p className="inline-note">{getPerformanceSummary(result, runtimeDistribution, text)}</p>

      <div className="table-like result-table-wrap">
        <div className="panel-heading-row responsive">
          <p className="panel-meta">{text('RESULT_PANEL_PREVIEW_META_LABEL', '결과 미리보기')}</p>
          <span className="subtle-chip">{text('RESULT_PANEL_ROW_COUNT_LABEL', { count: result.rows.length }, `${result.rows.length}행`)}</span>
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
