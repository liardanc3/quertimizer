import { useUiText } from '../../lib/uiText';
import type { ProblemDetail } from '../../types/domain';

interface ProblemInfoPanelProps {
  problem: ProblemDetail;
}

function parseSchemaEntries(schemaInfo: string) {
  return schemaInfo
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const match = line.match(/^([^(]+)\((.+)\)$/);

      if (!match) {
        return {
          name: line,
          columns: [],
        };
      }

      return {
        name: match[1].trim(),
        columns: match[2]
          .split(',')
          .map((column) => column.trim())
          .filter(Boolean),
      };
    });
}

export default function ProblemInfoPanel({ problem }: ProblemInfoPanelProps) {
  const { text } = useUiText();
  const schemaEntries = parseSchemaEntries(problem.schemaInfo);
  const checklistItems = [
    text('PROBLEM_INFO_CHECKLIST_ALIAS_ITEM', '출력 컬럼 순서와 alias를 예시와 맞췄는지 확인하기'),
    text('PROBLEM_INFO_CHECKLIST_SCAN_ITEM', '조건과 조인을 먼저 줄여 불필요한 전체 스캔을 피하기'),
    text('PROBLEM_INFO_CHECKLIST_TAGS_ITEM', { tags: problem.tags.slice(0, 4).join(', ') }, `핵심 키워드 ${problem.tags.slice(0, 4).join(', ')}를 중심으로 풀이 방향 잡기`),
  ];

  return (
    <section className="panel-card">
      <div className="panel-heading-row responsive">
        <div>
          <p className="panel-meta">{text('PROBLEM_INFO_META_LABEL', '문제 요약')}</p>
          <h2 className="panel-title">{text('PROBLEM_INFO_TITLE', '문제 설명')}</h2>
        </div>
        <div className="solve-panel-badge-row">
          <span className="difficulty-chip">{problem.difficulty}</span>
          <span className="subtle-chip">{text('PROBLEM_INFO_TAG_COUNT_LABEL', { count: problem.tags.length }, `태그 ${problem.tags.length}개`)}</span>
        </div>
      </div>

      <div className="solve-description-stack">
        <p className="content-text">{problem.preview}</p>
        <p className="content-text">{problem.description}</p>
      </div>

      <div className="solve-info-grid">
        <article className="solve-subcard">
          <p className="solve-section-title">{text('PROBLEM_INFO_SCHEMA_SECTION_TITLE', '핵심 스키마')}</p>
          <div className="solve-schema-list">
            {schemaEntries.map((entry) => (
              <div key={entry.name} className="solve-schema-item">
                <strong>{entry.name}</strong>
                <span className="solve-schema-columns">
                  {entry.columns.length > 0 ? entry.columns.join(', ') : text('PROBLEM_INFO_SCHEMA_EMPTY_MESSAGE', '테이블 정의를 확인해 주세요.')}
                </span>
              </div>
            ))}
          </div>
        </article>

        <article className="solve-subcard">
          <p className="solve-section-title">{text('PROBLEM_INFO_CHECKLIST_SECTION_TITLE', '제출 체크리스트')}</p>
          <div className="solve-checklist">
            {checklistItems.map((item) => (
              <div key={item} className="solve-check-item">
                <span className="solve-check-icon" aria-hidden="true">
                  ✓
                </span>
                <span>{item}</span>
              </div>
            ))}
          </div>
        </article>
      </div>

      <div className="solve-example-grid">
        <article className="solve-subcard">
          <p className="solve-section-title">{text('PROBLEM_INFO_INPUT_EXAMPLE_TITLE', '입력 예시')}</p>
          <pre className="code-block solve-example-block">{problem.inputExample}</pre>
        </article>

        <article className="solve-subcard">
          <p className="solve-section-title">{text('PROBLEM_INFO_OUTPUT_EXAMPLE_TITLE', '출력 예시')}</p>
          <pre className="code-block solve-example-block">{problem.outputExample}</pre>
        </article>
      </div>
    </section>
  );
}
