interface ExecutionPlanDetailSection {
  sectionKey: string;
  sectionLabel: string;
  labels: string[];
}

interface ExecutionPlanDetailBoardProps {
  sections: ExecutionPlanDetailSection[];
  noneLabel: string;
  className?: string;
}

export function ExecutionPlanDetailBoard({ sections, noneLabel, className = '' }: ExecutionPlanDetailBoardProps) {
  return (
    <div className={`execution-plan-detail-board ${className}`.trim()}>
      {sections.map((section) => {
        const isEmpty = section.labels.length === 0;

        return (
          <div key={`${section.sectionKey}-${section.sectionLabel}`} className="execution-plan-detail-row">
            <span className="execution-plan-detail-label">{section.sectionLabel}</span>
            <span className={`execution-plan-detail-values ${isEmpty ? 'is-empty' : 'is-selected'}`.trim()}>
              {(isEmpty ? [noneLabel] : section.labels).join(', ')}
            </span>
          </div>
        );
      })}
    </div>
  );
}
