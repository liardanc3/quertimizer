import { useState } from 'react';
import DbmsSelector from '../components/problem/DbmsSelector';
import ProblemInfoPanel from '../components/problem/ProblemInfoPanel';
import ResultPanel from '../components/problem/ResultPanel';
import SqlEditorPanel from '../components/problem/SqlEditorPanel';
import { mockProblemDetailById, mockProblemDetails } from '../mocks/problemDetail';
import { navigate } from '../lib/navigation';
import type { DbmsType, MockResult } from '../types/domain';

interface ProblemSolvePageProps {
  problemId: string;
}

export default function ProblemSolvePage({ problemId }: ProblemSolvePageProps) {
  const problem = mockProblemDetailById[problemId] ?? mockProblemDetails[0];
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>('postgresql');
  const [sql, setSql] = useState(problem.starterSql);
  const [actionLabel, setActionLabel] = useState('초기 상태');
  const [result, setResult] = useState<MockResult>(problem.mockResult);

  return (
    <div className="page-stack">
      <div className="panel-heading-row responsive">
        <button type="button" className="btn ghost" onClick={() => navigate('/')}>
          ← 문제 목록
        </button>
        <p className="hint-text">TODO: API 연동 시 문제 진행률/제출 기록 조회</p>
      </div>

      <section className="panel-card compact">
        <p className="problem-number">RDBMS 트랙</p>
        <h1 className="page-title">문제 {problem.number}. {problem.title}</h1>
      </section>

      <DbmsSelector selectedDbms={selectedDbms} onChange={setSelectedDbms} disabledDbms={problem.disabledDbms} />

      <div className="solve-grid">
        <ProblemInfoPanel problem={problem} />
        <SqlEditorPanel
          sql={sql}
          setSql={setSql}
          initialSql={problem.starterSql}
          onRun={() => {
            setActionLabel('실행');
            setResult(problem.mockResult);
          }}
          onSubmit={() => {
            setActionLabel('제출');
            setResult(problem.mockResult);
          }}
        />
        <ResultPanel result={result} actionLabel={actionLabel} />
      </div>
    </div>
  );
}
