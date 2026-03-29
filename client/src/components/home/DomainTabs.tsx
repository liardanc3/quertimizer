import type { DomainType } from '../../types/domain';

interface DomainTabsProps {
  selectedDomain: DomainType;
  onChange: (domain: DomainType) => void;
}

export default function DomainTabs({ selectedDomain, onChange }: DomainTabsProps) {
  const tabs = [
    { id: 'rdbms' as const, label: 'RDBMS', disabled: false },
    { id: 'nosql' as const, label: 'NoSQL', disabled: true },
  ];

  return (
    <div aria-label="문제 분류 선택" role="tablist" className="tabs-list problem-domain-tabs">
      {tabs.map((tab) => {
        const isSelected = selectedDomain === tab.id;

        return (
          <button
            key={tab.id}
            id={`tab-${tab.id}`}
            type="button"
            role="tab"
            aria-selected={isSelected}
            aria-controls={`panel-${tab.id}`}
            disabled={tab.disabled}
            tabIndex={isSelected ? 0 : -1}
            onClick={() => onChange(tab.id)}
            className={`tab-button ${isSelected ? 'is-selected' : ''}`}
          >
            {tab.label}
            {tab.disabled && <span className="tab-meta">준비중</span>}
          </button>
        );
      })}
    </div>
  );
}
