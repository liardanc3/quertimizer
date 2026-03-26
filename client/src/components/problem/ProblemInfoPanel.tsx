import type { ProblemDetail } from '../../types/domain';

interface ProblemInfoPanelProps {
  problem: ProblemDetail;
}

export default function ProblemInfoPanel({ problem }: ProblemInfoPanelProps) {
  return (
    <section className="panel-card">
      <div className="panel-heading-row responsive">
        <div>
          <p className="panel-meta">Problem Brief</p>
          <h2 className="panel-title">문제 설명</h2>
        </div>
        <span className="difficulty-chip">{problem.difficulty}</span>
      </div>

      <div className="tag-row">
        {problem.tags.map((tag) => (
          <span key={tag} className="tag-item">
            #{tag}
          </span>
        ))}
      </div>

      <p className="content-text">{problem.description}</p>

      <h3 className="panel-meta">스키마</h3>
      <pre className="code-block">{problem.schemaInfo}</pre>

      <h3 className="panel-meta">입력 예시</h3>
      <pre className="code-block">{problem.inputExample}</pre>

      <h3 className="panel-meta">출력 예시</h3>
      <pre className="code-block">{problem.outputExample}</pre>
    </section>
  );
}
