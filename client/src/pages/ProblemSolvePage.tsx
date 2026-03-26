import { useState } from 'react';
import DbmsSelector from '../components/problem/DbmsSelector';
import ProblemInfoPanel from '../components/problem/ProblemInfoPanel';
import ResultPanel from '../components/problem/ResultPanel';
import SqlEditorPanel from '../components/problem/SqlEditorPanel';
import { PROBLEMS_PATH, navigate } from '../lib/navigation';
import { mockProblemDetailById, mockProblemDetails } from '../mocks/problemDetail';
import type { DbmsType, MockResult } from '../types/domain';

interface ProblemSolvePageProps {
  problemId: string;
}

export default function ProblemSolvePage({ problemId }: ProblemSolvePageProps) {
  const problem = mockProblemDetailById[problemId] ?? mockProblemDetails[0];
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>('postgresql');
  const [sql, setSql] = useState(problem.starterSql);
  const [actionLabel, setActionLabel] = useState('대기 중');
  const [result, setResult] = useState<MockResult>(problem.mockResult);

  return (
    <div className="page-stack">
      <section className="panel-card">
        <div className="panel-heading-row responsive">
          <div>
            <button type="button" className="btn text inline" onClick={() => navigate(PROBLEMS_PATH)}>
              문제 목록으로
            </button>
            <p className="problem-number">RDBMS 트랙 · 문제 {problem.number}</p>
            <h1 className="page-title">{problem.title}</h1>
            <p className="muted-text">정답과 성능 지표를 함께 보는 SQL 문제 풀이 워크스페이스입니다.</p>
          </div>

          <div className="solve-meta-wrap">
            <span className="difficulty-chip">{problem.difficulty}</span>
            {problem.tags.slice(0, 3).map((tag) => (
              <span key={tag} className="tag-item">
                #{tag}
              </span>
            ))}
          </div>
        </div>
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
