import { useState } from 'react';
import ProblemRuntimeChart from '../components/home/ProblemRuntimeChart';
import DbmsSelector from '../components/problem/DbmsSelector';
import ProblemInfoPanel from '../components/problem/ProblemInfoPanel';
import ResultPanel from '../components/problem/ResultPanel';
import SqlEditorPanel from '../components/problem/SqlEditorPanel';
import { PROBLEMS_PATH, navigate } from '../lib/navigation';
import { mockProblemDetailById, mockProblemDetails } from '../mocks/problemDetail';
import type { DbmsType, ProblemDetail, RuntimeDistribution } from '../types/domain';

interface ProblemSolvePageProps {
  problemId: string;
}

const countFormatter = new Intl.NumberFormat('ko-KR');
const actionTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

function getDbmsLabel(dbms: DbmsType) {
  return dbms === 'postgresql' ? 'PostgreSQL' : 'Oracle';
}

function getAvailableDbms(problem: ProblemDetail) {
  return problem.dbmsOptions.filter((dbms) => !problem.disabledDbms.includes(dbms));
}

function formatActionTime(date: Date) {
  return actionTimeFormatter.format(date);
}

function formatDistributionValue(value?: number) {
  if (value === undefined) {
    return '-';
  }

  return `${countFormatter.format(value)}ms`;
}

function getRuntimeDistribution(problem: ProblemDetail, selectedDbms: DbmsType): RuntimeDistribution | undefined {
  return problem.runtimeDistributions?.[selectedDbms] ?? problem.runtimeDistribution;
}

export default function ProblemSolvePage({ problemId }: ProblemSolvePageProps) {
  const problem = mockProblemDetailById[problemId] ?? mockProblemDetails[0];
  const availableDbms = getAvailableDbms(problem);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>(availableDbms[0] ?? problem.dbmsOptions[0] ?? 'postgresql');
  const [sql, setSql] = useState(problem.starterSql);
  const [actionLabel, setActionLabel] = useState('실행 대기');
  const [lastActionAt, setLastActionAt] = useState<string | null>(null);
  const [result, setResult] = useState(problem.mockResult);

  const runtimeDistribution = getRuntimeDistribution(problem, selectedDbms);
  const summaryItems = [
    {
      label: '현재 DBMS',
      value: getDbmsLabel(selectedDbms),
      caption:
        problem.disabledDbms.length > 0
          ? `${problem.disabledDbms.length}개 환경은 준비 중입니다`
          : '선택한 DBMS 기준으로 제출 결과를 비교합니다',
    },
    {
      label: '누적 제출',
      value: runtimeDistribution ? `${countFormatter.format(runtimeDistribution.submissionCount)}회` : '-',
      caption:
        problem.solvedCount > 0
          ? `${countFormatter.format(problem.solvedCount)}명이 해결한 문제입니다`
          : '아직 해결 데이터가 없는 신규 문제입니다',
    },
    {
      label: '최고 기록',
      value: formatDistributionValue(runtimeDistribution?.fastestTimeMs),
      caption: runtimeDistribution ? runtimeDistribution.fastestNickname : '첫 기록을 기다리는 중',
    },
    {
      label: '내 최고',
      value: formatDistributionValue(runtimeDistribution?.myTimeMs),
      caption: problem.solvedAt ? `마지막 해결 ${problem.solvedAt}` : '아직 해결 전입니다',
    },
  ];

  const handleRun = () => {
    setActionLabel('실행');
    setLastActionAt(formatActionTime(new Date()));
    setResult(problem.mockResult);
  };

  const handleSubmit = () => {
    setActionLabel('제출');
    setLastActionAt(formatActionTime(new Date()));
    setResult(problem.mockResult);
  };

  return (
    <div className="page-stack">
      <section className="panel-card solve-hero-card">
        <div className="solve-hero-header">
          <button type="button" className="btn text inline" onClick={() => navigate(PROBLEMS_PATH)}>
            문제 목록으로
          </button>

          <div className="solve-hero-status">
            <span className="subtle-chip">{lastActionAt ? `${actionLabel} · ${lastActionAt}` : '아직 실행 전'}</span>
            {problem.solvedAt ? (
              <span className="problem-solved-badge is-solved">해결 완료</span>
            ) : (
              <span className="problem-solved-badge is-unsolved">미해결</span>
            )}
          </div>
        </div>

        <div className="solve-hero-copy">
          <p className="problem-number">RDBMS 문제 {problem.number}</p>
          <h1 className="page-title">{problem.title}</h1>
          <p className="muted-text">
            정답뿐 아니라 실행 계획과 성능까지 함께 보는 SQL 제출 화면입니다. 문제 설명, 실행 환경, 채점 결과를 한
            화면 안에서 이어서 확인할 수 있게 구성했습니다.
          </p>
        </div>

        <div className="solve-meta-wrap">
          <span className="difficulty-chip">{problem.difficulty}</span>
          {problem.tags.slice(0, 5).map((tag) => (
            <span key={tag} className="tag-item">
              #{tag}
            </span>
          ))}
        </div>

        <div className="solve-summary-grid">
          {summaryItems.map((item) => (
            <article key={item.label} className="solve-summary-card">
              <p className="solve-summary-label">{item.label}</p>
              <strong className="solve-summary-value">{item.value}</strong>
              <p className="solve-summary-caption">{item.caption}</p>
            </article>
          ))}
        </div>
      </section>

      <DbmsSelector
        selectedDbms={selectedDbms}
        onChange={setSelectedDbms}
        supportedDbms={problem.dbmsOptions}
        disabledDbms={problem.disabledDbms}
      />

      <div className="solve-grid">
        <ProblemInfoPanel problem={problem} />
        <SqlEditorPanel
          sql={sql}
          setSql={setSql}
          initialSql={problem.starterSql}
          selectedDbms={selectedDbms}
          actionLabel={actionLabel}
          lastActionAt={lastActionAt}
          onRun={handleRun}
          onSubmit={handleSubmit}
        />
        <ResultPanel
          problem={problem}
          result={result}
          actionLabel={actionLabel}
          selectedDbms={selectedDbms}
          lastActionAt={lastActionAt}
        />
      </div>

      <section className="panel-card solve-runtime-panel">
        <div className="panel-heading-row responsive">
          <div>
            <p className="panel-meta">Submission Benchmark</p>
            <h2 className="panel-title">실행 분포 비교</h2>
          </div>
          <div className="solve-panel-badge-row">
            <span className="subtle-chip">{getDbmsLabel(selectedDbms)}</span>
            <span className="subtle-chip">
              {runtimeDistribution
                ? `표본 ${countFormatter.format(runtimeDistribution.samples.length)}건`
                : '비교할 제출 데이터 없음'}
            </span>
          </div>
        </div>
        <p className="content-text">
          같은 문제를 푼 다른 제출과 실행시간, Scan Rows, 실행 계획 유형을 비교해 현재 쿼리의 위치를 빠르게 파악할 수
          있습니다.
        </p>
        <ProblemRuntimeChart problem={problem} onSearchSelect={() => undefined} />
      </section>
    </div>
  );
}
